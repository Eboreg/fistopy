package us.huseli.fistopy

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import us.huseli.fistopy.dataclasses.radio.RadioCombo
import us.huseli.fistopy.dataclasses.radio.RadioTrackCombo
import us.huseli.fistopy.dataclasses.spotify.SpotifyTrack
import us.huseli.fistopy.dataclasses.track.ISavedTrackCombo
import us.huseli.fistopy.dataclasses.track.QueueTrackCombo
import us.huseli.fistopy.enums.RadioType
import us.huseli.fistopy.interfaces.ILogger
import us.huseli.fistopy.repositories.Repositories
import kotlin.math.min
import kotlin.random.Random

class RadioTrackChannel(val radio: RadioCombo, private val repos: Repositories) : AbstractScopeHolder(), ILogger {
    private val usedSpotifyTrackIds = mutableSetOf<String>()
    private val usedLocalTrackIds = mutableSetOf<String>()
    private var fetchLoopJob: Job? = null

    val channel = Channel<RadioTrackCombo>()

    init {
        repos.message.onActivateRadio(radio)
        usedSpotifyTrackIds.addAll(radio.usedSpotifyTrackIds)
        usedLocalTrackIds.addAll(radio.usedLocalTrackIds.filterNotNull())

        fetchLoopJob = launchOnIOThread { startLoop() }
    }

    fun cancel() {
        fetchLoopJob?.cancel()
        channel.close()
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun enqueueNext(spotifyTrack: suspend () -> SpotifyTrack) {
        var combo: RadioTrackCombo? = null

        while (combo == null) {
            combo =
                if (radio.type == RadioType.LIBRARY && nextIsLibraryTrack()) {
                    getRandomLibraryQueueTrackCombo()
                        ?.let { RadioTrackCombo(queueTrackCombo = it, localId = it.track.trackId) }
                } else {
                    spotifyTrackToQueueTrackCombo(spotifyTrack())
                        ?.let { RadioTrackCombo(queueTrackCombo = it, spotifyId = it.track.spotifyId) }
                }
        }

        channel.send(combo)
        combo.localId?.also { usedLocalTrackIds.add(it) }
        combo.spotifyId?.also { usedSpotifyTrackIds.add(it) }
    }

    private suspend fun getQueueTrackCombo(trackCombo: ISavedTrackCombo<*>): QueueTrackCombo? {
        val newTrack = repos.youtube.ensureTrackPlayUri(trackCombo) { repos.track.upsertTrack(it) }

        return newTrack.playUri?.let { uri ->
            QueueTrackCombo(
                track = newTrack,
                uri = uri,
                album = trackCombo.album,
                albumArtists = trackCombo.albumArtists,
                trackArtists = trackCombo.trackArtists,
            )
        }
    }

    private suspend fun getRandomLibraryQueueTrackCombo(): QueueTrackCombo? {
        // Does a Youtube match if necessary.
        val usedTrackIds = usedLocalTrackIds.toMutableSet()

        while (true) {
            val combos = repos.track.listRandomLibraryTrackCombos(
                limit = 10,
                exceptTrackIds = usedTrackIds.toList(),
                exceptSpotifyTrackIds = usedSpotifyTrackIds.toList(),
            )

            if (combos.isEmpty()) return null

            for (combo in combos) {
                getQueueTrackCombo(combo)?.also { return it }

                usedTrackIds.add(combo.track.trackId)
            }
        }
    }

    private suspend fun listRandomLibrarySpotifyTrackIds(limit: Int): List<String> {
        /**
         * Returns the Spotify ID's of up to `limit` random tracks from the library. To be used as seed for Spotify's
         * track recommendation API when "library radio" is activated.
         */
        val trackCount = repos.track.getLibraryTrackCount()
        val spotifyTrackIds = mutableListOf<String>()
        var triedTrackCount = 0
        val trueLimit = limit.coerceAtMost(trackCount)

        while (spotifyTrackIds.size < trueLimit && triedTrackCount < trackCount) {
            for (trackCombo in repos.track.listRandomLibraryTrackCombos(min(limit * 4, trackCount))) {
                if (trackCombo.track.spotifyId != null) spotifyTrackIds.add(trackCombo.track.spotifyId)
                else {
                    val spotifyTrack = repos.spotify.matchTrack(trackCombo)?.externalData

                    if (spotifyTrack != null) {
                        spotifyTrackIds.add(spotifyTrack.id)
                        repos.track.setTrackSpotifyId(trackCombo.track.trackId, spotifyTrack.id)
                    }
                }
                triedTrackCount++
                if (spotifyTrackIds.size >= trueLimit) return spotifyTrackIds.toList()
            }
        }

        return spotifyTrackIds.toList()
    }

    private fun nextIsLibraryTrack(): Boolean = Random.nextFloat() >= repos.settings.libraryRadioNovelty.value

    private suspend fun spotifyTrackToQueueTrackCombo(spotifyTrack: SpotifyTrack): QueueTrackCombo? {
        val unsavedAlbum = spotifyTrack.album.toAlbum(isInLibrary = false, isLocal = false)
        val trackCombo = spotifyTrack.toTrackCombo(album = unsavedAlbum)
        val track = repos.youtube.getBestTrackMatch(trackCombo)
        val playUri = track?.playUri

        if (track != null && playUri != null) {
            val album = repos.album.getOrCreateAlbumBySpotifyId(unsavedAlbum, spotifyTrack.album.id)
            val albumArtists =
                trackCombo.albumArtists.map { artistCredit -> artistCredit.withAlbumId(album.albumId) }
            val albumArtistCredits = repos.artist.insertAlbumArtists(albumArtists)
            val updatedTrack = track.copy(albumId = album.albumId).also { repos.track.upsertTrack(it) }
            val trackArtistCredits = repos.artist.insertTrackArtists(trackCombo.trackArtists)

            return QueueTrackCombo(
                track = updatedTrack,
                album = album,
                uri = playUri,
                trackArtists = trackArtistCredits,
                albumArtists = albumArtistCredits,
            )
        }
        return null
    }

    private suspend fun startLoop() {
        try {
            val recommendationsChannel = if (!radio.isInitialized || usedSpotifyTrackIds.size < 5) {
                when (radio.type) {
                    RadioType.LIBRARY -> repos.spotify.trackRecommendationsChannel(
                        listRandomLibrarySpotifyTrackIds(5)
                    )
                    RadioType.ARTIST -> radio.artist?.let { repos.spotify.trackRecommendationsChannelByArtist(it) }
                    RadioType.ALBUM -> radio.album?.let { album ->
                        repos.album.getAlbumWithTracks(album.albumId)
                            ?.let { repos.spotify.trackRecommendationsChannelByAlbumCombo(it) }
                    }
                    RadioType.TRACK -> radio.track?.let { track ->
                        val trackCombo = repos.track.getTrackComboById(track.trackId)
                        trackCombo?.let { repos.spotify.trackRecommendationsChannelByTrack(it) }
                    }
                }
            } else repos.spotify.trackRecommendationsChannel(usedSpotifyTrackIds)

            while (recommendationsChannel != null) {
                enqueueNext { recommendationsChannel.receive() }
            }
        } catch (e: ClosedReceiveChannelException) {
            channel.close()
        } catch (e: Throwable) {
            logError(e)
        }
    }
}
