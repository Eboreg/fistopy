package us.huseli.fistopy.managers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.AlbumDownloadTask
import us.huseli.fistopy.Constants.MAX_CONCURRENT_TRACK_DOWNLOADS
import us.huseli.fistopy.DownloadTaskState
import us.huseli.fistopy.R
import us.huseli.fistopy.TrackDownloadTask
import us.huseli.fistopy.XSPFPlaylist
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.ProgressData
import us.huseli.fistopy.dataclasses.album.AlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.withUpdates
import us.huseli.fistopy.dataclasses.artist.ITrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.musicbrainz.capitalizeGenreName
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.toMediaStoreImage
import us.huseli.fistopy.dataclasses.track.ITrackCombo
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.dataclasses.track.listCoverImages
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.TrackMergeStrategy
import us.huseli.fistopy.getBitmap
import us.huseli.fistopy.getSquareSize
import us.huseli.fistopy.interfaces.ILogger
import us.huseli.fistopy.repositories.Repositories
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LibraryManager @Inject constructor(
    private val repos: Repositories,
    private val database: Database,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), ILogger {
    private val _albumDownloadTasks = MutableStateFlow<List<AlbumDownloadTask>>(emptyList())
    private val _trackDownloadTasks = MutableStateFlow<ImmutableList<TrackDownloadTask>>(persistentListOf())
    private val _runningTasks = MutableStateFlow<List<TrackDownloadTask>>(emptyList())

    val trackDownloadTasks = _trackDownloadTasks.asStateFlow()

    init {
        doStartupTasks()

        launchOnIOThread {
            _runningTasks.collect { runningTasks ->
                if (runningTasks.size < MAX_CONCURRENT_TRACK_DOWNLOADS) {
                    _trackDownloadTasks.value.find { it.state.value == DownloadTaskState.CREATED }?.start()
                }
            }
        }

        repos.musicBrainz.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, musicBrainzId ->
            repos.artist.setArtistMusicBrainzId(artistId, musicBrainzId)
        }

        repos.spotify.startMatchingArtists(repos.artist.artistsWithTracksOrAlbums) { artistId, spotifyId, image ->
            repos.artist.setArtistSpotifyData(artistId, spotifyId, image)
        }
    }


    /** PUBLIC METHODS ************************************************************************************************/

    suspend fun addAlbumsToLibrary(
        albumIds: List<String>,
        onGotoLibraryClick: (() -> Unit)? = null,
        onGotoAlbumClick: ((String) -> Unit)? = null,
    ) {
        for (combo in repos.album.listAlbumsWithTracks(albumIds)) {
            upsertAlbumWithTracks(combo.withUpdates { setIsInLibrary(true) })
        }
        repos.message.onAddAlbumsToLibrary(albumIds, onGotoLibraryClick, onGotoAlbumClick)
    }

    fun addTemporaryMusicBrainzAlbum(releaseGroupId: String, onFinish: (String) -> Unit) {
        launchOnIOThread {
            val releaseGroup = repos.musicBrainz.getReleaseGroup(releaseGroupId)

            repos.musicBrainz.listReleasesByReleaseGroupId(releaseGroupId).firstOrNull()?.also { release ->
                val albumCombo =
                    release.toAlbumWithTracks(isLocal = false, isInLibrary = false, releaseGroupId = releaseGroup?.id)
                val album = repos.album.getOrCreateAlbumByMusicBrainzId(albumCombo.album, releaseGroupId, release.id)
                val albumArtists = albumCombo.artists.map { it.withAlbumId(album.albumId) }

                repos.artist.insertAlbumArtists(albumArtists)
                onMainThread { onFinish(album.albumId) }
            }
        }
    }

    fun cancelAlbumDownload(albumId: String) {
        _albumDownloadTasks.value.find { it.album.albumId == albumId }?.cancel()
    }

    suspend fun deleteLocalAlbumFiles(albumIds: Collection<String>) {
        val combos = repos.album.listAlbumsWithTracks(albumIds)
        val tracks = combos.flatMap { it.tracks }

        if (tracks.isNotEmpty()) repos.track.deleteTrackFiles(tracks)
        if (combos.isNotEmpty()) database.withTransaction {
            repos.album.setAlbumsIsLocal(combos.map { it.album.albumId }, false)
            combos.filter { it.album.albumArt?.isLocal == true }.forEach { repos.album.clearAlbumArt(it.album.albumId) }
            if (tracks.isNotEmpty()) repos.track.clearLocalUris(tracks.map { it.trackId })
        }
    }

    fun doStartupTasks() {
        launchOnIOThread {
            updateGenreList()
            if (repos.settings.autoImportLocalMusic.value == true) importNewLocalAlbums()
            handleOrphansAndDuplicates()
            repos.playlist.deleteOrphanPlaylistTracks()
            repos.track.deleteTempTracks()
            repos.album.deleteTempAlbums()
            // repos.artist.deleteOrphanArtists()
        }
    }

    fun downloadAlbum(
        albumId: String,
        onFinish: (AlbumDownloadTask.Result) -> Unit,
        onTrackError: (TrackCombo, Throwable) -> Unit,
    ) {
        launchOnIOThread {
            repos.album.getAlbumWithTracks(albumId)
                ?.let { it.copy(album = it.album.copy(isLocal = true, isInLibrary = true)) }
                ?.also { albumCombo ->
                    repos.settings
                        .createAlbumDirectory(albumCombo.album.title, albumCombo.artists.joined())
                        ?.also { directory ->
                            val trackCombos = albumCombo.trackCombos
                                .filter { !it.track.isDownloaded && it.track.isOnYoutube }
                            val trackTasks = trackCombos.map { trackCombo ->
                                createTrackDownloadTask(
                                    combo = trackCombo,
                                    directory = directory,
                                    onError = { onTrackError(trackCombo, it) },
                                )
                            }

                            _albumDownloadTasks.value += AlbumDownloadTask(
                                album = albumCombo.album,
                                trackTasks = trackTasks,
                                onFinish = { result ->
                                    if (result.succeededTracks.isNotEmpty()) launchOnIOThread {
                                        upsertAlbumWithTracks(albumCombo.withUpdates { setIsInLibrary(true) })
                                    }
                                    onFinish(result)
                                },
                            )
                        }
                }
        }
    }

    fun downloadTrack(trackId: String) {
        launchOnIOThread {
            repos.settings.getLocalMusicDirectory()?.also { directory ->
                repos.track.getTrackComboById(trackId)?.also { combo ->
                    createTrackDownloadTask(combo = combo, directory = directory)
                }
            }
        }
    }

    suspend fun ensureTrackMetadata(track: Track, forceReload: Boolean = false, commit: Boolean = true): Track =
        repos.youtube.ensureTrackMetadata(track = track, forceReload = forceReload) {
            if (commit) repos.track.upsertTrack(it)
        }

    fun ensureTrackMetadataAsync(trackId: String) {
        launchOnIOThread {
            repos.track.getTrackById(trackId)?.also { ensureTrackMetadata(track = it) }
        }
    }

    fun exportTracksAsJspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        title: String? = null,
    ): Boolean {
        val jspf = XSPFPlaylist.fromTrackCombos(combos = trackCombos, title = title, dateTime = dateTime).toJson()

        return context.contentResolver.openAssetFileDescriptor(outputUri, "wt")
            ?.createOutputStream()
            ?.bufferedWriter()
            ?.use {
                it.write(jspf)
                true
            } ?: false
    }

    fun exportTracksAsXspf(
        trackCombos: Collection<TrackCombo>,
        outputUri: Uri,
        dateTime: OffsetDateTime,
        title: String? = null,
    ): Boolean {
        val xspf = XSPFPlaylist.fromTrackCombos(combos = trackCombos, title = title, dateTime = dateTime).toXml()

        return xspf?.let {
            context.contentResolver.openAssetFileDescriptor(outputUri, "wt")
                ?.createOutputStream()
                ?.bufferedWriter()
                ?.use { writer ->
                    writer.write(it)
                    true
                } ?: false
        } ?: false
    }

    fun getAlbumDownloadUiStateFlow(albumId: String) = _albumDownloadTasks
        .flatMapLatest { tasks -> tasks.find { it.album.albumId == albumId }?.uiStateFlow ?: emptyFlow() }

    fun getTrackDownloadUiStateFlow(trackId: String) = _trackDownloadTasks
        .flatMapLatest { tasks -> tasks.find { it.track.trackId == trackId }?.uiStateFlow ?: emptyFlow() }

    suspend fun importNewLocalAlbums() {
        if (!repos.localMedia.isImportingLocalMedia.value) {
            val localMusicDirectory =
                repos.settings.localMusicUri.value?.let { DocumentFile.fromTreeUri(context, it) }

            if (localMusicDirectory != null) {
                repos.localMedia.setIsImporting(true)

                val existingAlbumCombos = repos.album.listAlbumCombos()
                val existingTrackUris = repos.track.listTrackLocalUris()

                for (localCombo in repos.localMedia.importableAlbumsChannel(localMusicDirectory, existingTrackUris)) {
                    val existingCombo = existingAlbumCombos.find {
                        (it.album.title == localCombo.album.title && it.artists.joined() == localCombo.artists.joined()) ||
                            (it.album.musicBrainzReleaseId != null && it.album.musicBrainzReleaseId == localCombo.album.musicBrainzReleaseId)
                    }
                    val combo = if (existingCombo != null) {
                        localCombo.withUpdates {
                            updateAlbum {
                                it.copy(
                                    albumId = existingCombo.album.albumId,
                                    musicBrainzReleaseGroupId = it.musicBrainzReleaseGroupId
                                        ?: existingCombo.album.musicBrainzReleaseGroupId,
                                    musicBrainzReleaseId = it.musicBrainzReleaseId
                                        ?: existingCombo.album.musicBrainzReleaseId,
                                    year = it.year ?: existingCombo.album.year,
                                )
                            }
                            mergeArtists(existingCombo.artists, ListUpdateStrategy.MERGE)
                        }
                    } else localCombo
                    val albumArt = getBestNewLocalAlbumArt(combo.trackCombos)
                        ?: repos.musicBrainz.getCoverArtArchiveImage(
                            combo.album.musicBrainzReleaseId,
                            combo.album.musicBrainzReleaseGroupId
                        )?.toMediaStoreImage()

                    upsertAlbumWithTracks(combo.withUpdates { setAlbumArt(albumArt) })
                }

                repos.localMedia.setIsImporting(false)
            }
        }
    }

    fun matchUnplayableTracks(albumCombo: AlbumWithTracksCombo) = channelFlow<ProgressData> {
        val progressData = ProgressData(text = context.getString(R.string.matching), isActive = true).also { send(it) }
        val unplayableTrackIds = albumCombo.trackCombos.filter { !it.track.isPlayable }.map { it.track.trackId }
        val match = repos.youtube.getBestAlbumMatch(albumCombo) { progress ->
            trySend(progressData.copy(progress = progress * 0.5))
        }
        val matchedCombo = match?.albumCombo
        val updatedTracks = matchedCombo?.trackCombos
            ?.map { it.track }
            ?.filter { unplayableTrackIds.contains(it.trackId) }
            ?: emptyList()

        if (updatedTracks.isNotEmpty()) {
            send(progressData.copy(progress = 0.5, text = context.getString(R.string.importing)))
            repos.track.upsertTracks(updatedTracks.map { ensureTrackMetadata(it, commit = false) })
            send(progressData.copy(progress = 0.9, text = context.getString(R.string.importing)))
        }

        // So youtubePlaylist gets saved:
        matchedCombo?.album?.also { repos.album.upsertAlbum(it) }
        repos.message.onMatchUnplayableTracks(updatedTracks.size)
        send(ProgressData())
    }

    suspend fun updateTrack(
        trackId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        albumPosition: Int? = null,
        discNumber: Int? = null,
    ) {
        val trackCombo = repos.track.getTrackComboById(trackId)

        if (trackCombo != null) {
            var finalTrackArtists: List<ITrackArtistCredit> = trackCombo.trackArtists.toList()

            if (artistNames.filter { it.isNotEmpty() } != trackCombo.trackArtists.map { it.name }) {
                finalTrackArtists = artistNames.filter { it.isNotEmpty() }.mapIndexed { index, name ->
                    UnsavedTrackArtistCredit(
                        name = name,
                        trackId = trackCombo.track.trackId,
                        position = index,
                    )
                }

                repos.artist.setTrackArtists(trackCombo.track.trackId, finalTrackArtists)
            }

            val updatedTrack = ensureTrackMetadata(
                track = trackCombo.track.copy(
                    title = title,
                    year = year,
                    albumPosition = albumPosition ?: trackCombo.track.albumPosition,
                    discNumber = discNumber ?: trackCombo.track.discNumber,
                ),
                commit = false,
            )

            repos.track.upsertTrack(updatedTrack)
            repos.localMedia.tagTrack(
                track = updatedTrack,
                trackArtists = finalTrackArtists,
                album = trackCombo.album,
                albumArtists = trackCombo.albumArtists,
            )
        }
    }

    suspend fun upsertAlbumWithTracks(combo: IAlbumWithTracksCombo<*, *>) {
        database.withTransaction {
            val finalCombo = getRemoteAlbumWithTracks(combo) ?: combo

            repos.album.upsertAlbum(finalCombo.album)
            repos.album.setAlbumTags(finalCombo.album.albumId, combo.tags)
            repos.track.setAlbumTracks(finalCombo.album.albumId, finalCombo.tracks)
            repos.artist.setAlbumComboArtists(finalCombo)
        }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private fun createTrackDownloadTask(
        combo: ITrackCombo<*>,
        directory: DocumentFile,
        onError: (Throwable) -> Unit = {},
    ): TrackDownloadTask {
        val task = TrackDownloadTask(
            scope = scope,
            track = combo.track,
            trackArtists = combo.trackArtists,
            directory = directory,
            repos = repos,
            album = combo.album,
            albumArtists = combo.albumArtists,
            onError = onError,
        )

        _trackDownloadTasks.value = _trackDownloadTasks.value.plus(task).toImmutableList()
        if (_runningTasks.value.size < MAX_CONCURRENT_TRACK_DOWNLOADS) task.start()

        scope.launch {
            task.state.collect { state ->
                if (state == DownloadTaskState.RUNNING) {
                    if (!_runningTasks.value.contains(task)) _runningTasks.value += task
                } else _runningTasks.value -= task
            }
        }

        return task
    }

    private suspend fun getRemoteAlbumWithTracks(combo: IAlbumWithTracksCombo<*, *>): UnsavedAlbumWithTracksCombo? {
        val spotifyCombo = if (combo.album.spotifyId == null) repos.spotify.matchAlbumWithTracks(
            combo = combo,
            trackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
        ) else null
        val mbCombo = if (combo.album.musicBrainzReleaseId == null) repos.musicBrainz.matchAlbumWithTracks(
            combo = combo,
            trackMergeStrategy = TrackMergeStrategy.KEEP_SELF,
        ) else null

        return mbCombo ?: spotifyCombo
    }

    private suspend fun handleOrphansAndDuplicates() {
        val allAlbumsWithTracks = repos.album.listAlbumsWithTracks()
        val albumTracks = allAlbumsWithTracks.flatMap { combo -> combo.tracks }
        val nonAlbumTracks = repos.track.listNonAlbumTracks()
        val allTracks = albumTracks + nonAlbumTracks
        // Collect tracks with non-working localUris:
        val brokenUriTrackIds = repos.localMedia.listTracksWithBrokenLocalUris(allTracks).map { it.trackId }
        val nonLocalTrackIds = brokenUriTrackIds + allTracks.filter { it.localUri == null }.map { it.trackId }
        // Collect albums that have isLocal=true but should have false:
        val noLongerLocalAlbumsIds = allAlbumsWithTracks
            .filter { combo -> combo.album.isLocal && nonLocalTrackIds.containsAll(combo.trackIds) }
            .map { it.album.albumId }
        val duplicateNonAlbumTracks = nonAlbumTracks.filter { track ->
            albumTracks.find { it.localUri == track.localUri && it.youtubeVideo?.id == track.youtubeVideo?.id } != null
        }

        database.withTransaction {
            // Delete non-album tracks that have duplicates on albums:
            if (duplicateNonAlbumTracks.isNotEmpty())
                repos.track.deleteTracksById(duplicateNonAlbumTracks.map { it.trackId })
            // Update tracks with broken localUris:
            if (brokenUriTrackIds.isNotEmpty()) repos.track.clearLocalUris(brokenUriTrackIds)
            // Update albums that should have isLocal=true, but don't:
            if (noLongerLocalAlbumsIds.isNotEmpty()) repos.album.setAlbumsIsLocal(noLongerLocalAlbumsIds, false)
        }
    }

    private suspend fun getBestNewLocalAlbumArt(trackCombos: Collection<ITrackCombo<*>>): MediaStoreImage? =
        trackCombos.map { it.track }
            .listCoverImages(context)
            .map { it.uri.toMediaStoreImage() }
            .maxByOrNull { albumArt -> context.getBitmap(albumArt.fullUri)?.getSquareSize() ?: 0 }

    private suspend fun updateGenreList() {
        /** Fetches Musicbrainz' complete genre list. */
        try {
            val existingGenreNames = repos.album.listTagNames().map { it.lowercase() }.toSet()
            val mbGenreNames = repos.musicBrainz.listAllGenreNames()
            val newTags = mbGenreNames
                .minus(existingGenreNames)
                .map { Tag(name = capitalizeGenreName(it), isMusicBrainzGenre = true) }

            if (newTags.isNotEmpty()) repos.album.insertTags(newTags)
        } catch (e: Exception) {
            logError("updateGenreList: $e", e)
        }
    }
}
