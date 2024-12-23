package us.huseli.fistopy.repositories

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.Constants.CUSTOM_USER_AGENT
import us.huseli.fistopy.DeferredRequestJob
import us.huseli.fistopy.DeferredRequestJobManager
import us.huseli.fistopy.MutexCache
import us.huseli.fistopy.Request
import us.huseli.fistopy.dataclasses.coverartarchive.CoverArtArchiveImage
import us.huseli.fistopy.dataclasses.coverartarchive.CoverArtArchiveResponse
import us.huseli.fistopy.dataclasses.album.AlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.withUpdates
import us.huseli.fistopy.dataclasses.artist.Artist
import us.huseli.fistopy.dataclasses.artist.IArtist
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzArtistSearch
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRecordingSearch
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRelease
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseBrowse
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroup
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroupBrowse
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroupSearch
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseSearch
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.TrackMergeStrategy
import us.huseli.fistopy.externalcontent.SearchParams
import us.huseli.fistopy.interfaces.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicBrainzRepository @Inject constructor() : AbstractScopeHolder() {
    class RequestJob(url: String, lowPrio: Boolean = false) : DeferredRequestJob(url, lowPrio) {
        override suspend fun request(): String =
            Request(url = url, headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)).getString()
    }

    object RequestJobManager : DeferredRequestJobManager<RequestJob>(), ILogger {
        private var lastJobFinished: Long? = null

        override suspend fun waitBeforeUnlocking() {
            val timeSinceLast = lastJobFinished?.let { System.currentTimeMillis() - it }

            if (timeSinceLast != null && timeSinceLast < MIN_REQUEST_INTERVAL_MS) {
                delay(MIN_REQUEST_INTERVAL_MS - timeSinceLast)
            }
        }

        override fun onJobFinished(job: RequestJob, response: String?, timestamp: Long) {
            lastJobFinished = timestamp
        }
    }

    private val gson: Gson = GsonBuilder().create()
    private val apiResponseCache = MutexCache(
        itemToKey = { it.url },
        fetchMethod = { RequestJobManager.runJob(it) },
        debugLabel = "MusicBrainzRepository.apiResponseCache",
    )
    private var matchArtistsJob: Job? = null

    fun flowArtistReleaseGroups(artistId: String) = channelFlow<MusicBrainzReleaseGroupBrowse.ReleaseGroup> {
        var offset: Int? = 0

        while (offset != null) {
            val params = mapOf(
                "artist" to artistId,
                "release-group-status" to "website-default",
                "limit" to "100",
                "offset" to offset.toString(),
            )
            val response = gson.fromJson(
                request("release-group", params),
                MusicBrainzReleaseGroupBrowse::class.java,
            )

            for (releaseGroup in response.releaseGroups) {
                send(releaseGroup)
            }
            offset = (offset + response.releaseGroups.size).takeIf { it < response.releaseGroupCount }
        }
    }

    suspend fun getRelease(id: String): MusicBrainzRelease? = gson.fromJson(
        request("release/$id", mapOf("inc" to "recordings artist-credits genres release-groups labels")),
        MusicBrainzRelease::class.java,
    )

    suspend fun getReleaseGroup(id: String): MusicBrainzReleaseGroup? = gson.fromJson(
        request("release-group/$id", mapOf("inc" to "artist-credits genres releases")),
        MusicBrainzReleaseGroup::class.java,
    )

    suspend fun getReleaseId(combo: AlbumWithTracksCombo): String? = combo.album.musicBrainzReleaseId
        ?: getReleaseId(artist = combo.artists.joined(), album = combo.album.title, trackCount = combo.tracks.size)

    suspend fun getSiblingReleaseIds(releaseId: String): List<String> =
        getRelease(releaseId)?.releaseGroup?.id?.let { getReleaseGroup(it) }?.releases?.map { it.id } ?: emptyList()

    suspend fun listAllGenreNames(): Set<String> {
        val url = "$MUSICBRAINZ_API_ROOT/genre/all?fmt=txt"
        val headers = mapOf("User-Agent" to CUSTOM_USER_AGENT)

        return Request(url = url, headers = headers).getString().split('\n').toSet()
    }

    suspend fun listAllReleaseCoverArt(releaseId: String): List<CoverArtArchiveImage>? = Request(
        url = "https://coverartarchive.org/release/$releaseId",
        headers = mapOf("User-Agent" to CUSTOM_USER_AGENT),
    ).getObjectOrNull<CoverArtArchiveResponse>()?.images?.filter { it.front }

    suspend fun listReleasesByReleaseGroupId(releaseGroupId: String): List<MusicBrainzRelease> {
        val params = mapOf(
            "release-group" to releaseGroupId,
            "inc" to "recordings artist-credits genres labels release-groups",
            "status" to "official",
        )

        return gson.fromJson(
            request("release", params),
            MusicBrainzReleaseBrowse::class.java,
        ).releases
    }

    suspend fun matchAlbumWithTracks(
        combo: IAlbumWithTracksCombo<*, *>,
        maxDistance: Double = MAX_ALBUM_MATCH_DISTANCE,
        trackMergeStrategy: TrackMergeStrategy = TrackMergeStrategy.KEEP_LEAST,
        albumArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        trackArtistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        tagUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE,
    ): UnsavedAlbumWithTracksCombo? {
        val params = mutableMapOf(
            "release" to combo.album.title,
            "tracks" to combo.trackCombos.size.toString(),
        )
        combo.artists.joined()?.also { params["artist"] = it }

        val releases = gson.fromJson(
            search(resource = "release", queryParams = params),
            MusicBrainzReleaseSearch::class.java,
        )?.releases
        val releaseIds = releases?.map { it.id }
        val matches = releaseIds?.mapNotNull { releaseId ->
            getRelease(releaseId)?.toAlbumWithTracks(
                isLocal = combo.album.isLocal,
                isInLibrary = combo.album.isInLibrary,
            )?.match(combo)
        }
        val matchedCombo = matches?.filter { it.distance <= maxDistance }?.minByOrNull { it.distance }?.albumCombo
        val updatedCombo = matchedCombo?.let {
            combo.withUpdates {
                mergeAlbum(it.album)
                mergeTrackCombos(
                    other = it.trackCombos,
                    mergeStrategy = trackMergeStrategy,
                    artistUpdateStrategy = trackArtistUpdateStrategy,
                )
                mergeTags(it.tags, tagUpdateStrategy)
                mergeArtists(it.artists, albumArtistUpdateStrategy)
            }
        }
        val albumArt = updatedCombo?.album?.albumArt ?: getCoverArtArchiveImage(
            releaseId = updatedCombo?.album?.musicBrainzReleaseId,
            releaseGroupId = updatedCombo?.album?.musicBrainzReleaseGroupId,
        )?.toMediaStoreImage()

        return updatedCombo?.withUpdates { setAlbumArt(albumArt) }
    }

    fun recordingSearchChannel(searchParams: SearchParams) =
        Channel<ExternalTrackCombo<MusicBrainzRecordingSearch.Recording>>().also { channel ->
            launchOnIOThread {
                val params = mutableMapOf<String, String>()
                var total: Int?
                var offset = 0

                if (!searchParams.track.isNullOrEmpty()) params["title"] = searchParams.track
                if (!searchParams.artist.isNullOrEmpty()) params["artist"] = searchParams.artist
                if (!searchParams.album.isNullOrEmpty()) params["release"] = searchParams.album

                if (params.isNotEmpty() || !searchParams.freeText.isNullOrEmpty()) {
                    do {
                        val response = gson.fromJson(
                            search(
                                resource = "recording",
                                freeText = searchParams.freeText,
                                limit = 100,
                                queryParams = params,
                                offset = offset,
                            ),
                            MusicBrainzRecordingSearch::class.java,
                        )

                        total = response?.count
                        offset += 100
                        response?.recordings?.forEach { channel.send(it.toTrackCombo()) }
                    } while (total != null && total > (offset + 1) * 100)
                }
                channel.close()
            }
        }

    fun releaseGroupSearchChannel(searchParams: SearchParams) =
        Channel<ExternalAlbumWithTracksCombo<MusicBrainzReleaseGroupSearch.ReleaseGroup>>().also { channel ->
            launchOnIOThread {
                val params = mutableMapOf<String, String>()

                if (!searchParams.artist.isNullOrEmpty()) params["artist"] = searchParams.artist
                if (!searchParams.album.isNullOrEmpty()) params["release"] = searchParams.album

                if (params.isNotEmpty() || !searchParams.freeText.isNullOrEmpty()) {
                    var total: Int?
                    var offset = 0

                    do {
                        val response = gson.fromJson(
                            search(
                                resource = "release-group",
                                freeText = searchParams.freeText,
                                limit = 100,
                                queryParams = params,
                                offset = offset,
                            ),
                            MusicBrainzReleaseGroupSearch::class.java,
                        )

                        total = response?.count
                        offset += 100
                        response?.releaseGroups?.forEach { channel.send(it.toAlbumWithTracks()) }
                    } while (total != null && total > (offset + 1) * 100)
                }
                channel.close()
            }
        }

    suspend fun matchArtist(artist: IArtist, lowPrio: Boolean = false): MusicBrainzArtistSearch.Artist? {
        return search(resource = "artist", query = artist.name, lowPrio = lowPrio)
            ?.let { gson.fromJson(it, MusicBrainzArtistSearch::class.java) }
            ?.artists
            ?.firstOrNull { it.matches(artist.name) }
    }

    fun startMatchingArtists(flow: Flow<List<Artist>>, save: suspend (String, String) -> Unit) {
        if (matchArtistsJob == null) matchArtistsJob = launchOnIOThread {
            val previousIds = mutableSetOf<String>()

            flow
                .map { artists -> artists.filter { it.musicBrainzId == null && !previousIds.contains(it.artistId) } }
                .collect { artists ->
                    for (artist in artists) {
                        val match = matchArtist(artist = artist, lowPrio = true)

                        save(artist.artistId, match?.id ?: "")
                        previousIds.add(artist.artistId)
                    }
                }
        }
    }

    fun updateAlbumComboFromRelease(
        oldCombo: IAlbumWithTracksCombo<*, *>,
        release: MusicBrainzRelease,
    ): UnsavedAlbumWithTracksCombo {
        val newCombo = release.toAlbumWithTracks(
            isLocal = oldCombo.album.isLocal,
            isInLibrary = oldCombo.album.isInLibrary,
        )
        return oldCombo.withUpdates {
            mergeAlbum(newCombo.album)
            mergeArtists(newCombo.artists)
            mergeTags(newCombo.tags)
            mergeTrackCombos(
                other = newCombo.trackCombos,
                mergeStrategy = TrackMergeStrategy.KEEP_OTHER,
            )
            setAlbumArt(newCombo.album.albumArt)
            setAlbumTitle(newCombo.album.title)
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun escapeString(string: String): String =
        string.replace(Regex("([+\\-&|!(){}\\[\\]^\"~*?:\\\\/])"), "\\\\$1")
            .let { if (it.contains(' ')) "\"$it\"" else it }

    private suspend fun getAllReleaseGroupCoverArt(releaseGroupId: String): List<CoverArtArchiveImage>? = Request(
        url = "https://coverartarchive.org/release-group/$releaseGroupId",
        headers = mapOf("User-Agent" to CUSTOM_USER_AGENT),
    ).getObjectOrNull<CoverArtArchiveResponse>()?.images?.filter { it.front }

    suspend fun getCoverArtArchiveImage(releaseId: String?, releaseGroupId: String?): CoverArtArchiveImage? =
        releaseId?.let { listAllReleaseCoverArt(it)?.firstOrNull() }
            ?: releaseGroupId?.let { getAllReleaseGroupCoverArt(it)?.firstOrNull() }

    private suspend fun getReleaseId(artist: String?, album: String, trackCount: Int): String? {
        val params = mutableMapOf(
            "release" to album,
            "tracks" to trackCount.toString(),
        )
        if (artist != null) params["artist"] = artist
        val response =
            gson.fromJson(search(resource = "release", queryParams = params), MusicBrainzReleaseSearch::class.java)

        return response?.releases?.firstOrNull { release -> release.matches(artist, album) }?.id
    }

    private suspend fun request(
        path: String,
        params: Map<String, String> = emptyMap(),
        lowPrio: Boolean = false,
    ): String? {
        val url = Request.getUrl("$MUSICBRAINZ_API_ROOT/${path.trimStart('/')}", params.plus("fmt" to "json"))

        return apiResponseCache.getOrNull(RequestJob(url, lowPrio))
    }

    private suspend fun search(
        resource: String,
        freeText: String? = null,
        queryParams: Map<String, String> = emptyMap(),
        lowPrio: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
    ): String? {
        val query = queryParams
            .map { (key, value) -> "$key:${escapeString(value)}" }
            .let { if (freeText != null) it.plus(freeText.split(' ')) else it }
            .joinToString(" AND ")

        return request(
            path = resource,
            params = mapOf("query" to query, "limit" to limit.toString(), "offset" to offset.toString()),
            lowPrio = lowPrio,
        )
    }

    private suspend fun search(resource: String, query: String, lowPrio: Boolean = false, limit: Int = 10): String? {
        return request(resource, mapOf("query" to escapeString(query), "limit" to limit.toString()), lowPrio)
    }

    companion object {
        const val MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
        const val MAX_ALBUM_MATCH_DISTANCE = 1.0

        // https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting
        const val MIN_REQUEST_INTERVAL_MS = 1000L
    }
}
