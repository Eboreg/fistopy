package us.huseli.fistopy.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.EditAlbumUiState
import us.huseli.fistopy.dataclasses.album.IAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.toMediaStoreImage
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.dataclasses.track.listCoverImages
import us.huseli.fistopy.getBitmap
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.square
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditAlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    data class AlbumArt(
        val mediaStoreImage: MediaStoreImage,
        val bitmap: Bitmap,
        val isCurrent: Boolean = false,
    ) {
        override fun equals(other: Any?) = other is AlbumArt && other.mediaStoreImage == mediaStoreImage
        override fun hashCode() = mediaStoreImage.hashCode()
    }

    private val _albumId = MutableStateFlow<String?>(null)
    private val _isLoadingAlbumArt = MutableStateFlow(false)
    private val albumArtFetchJobs = mutableMapOf<String, List<Job>>()

    val allTags = repos.album.flowTags().stateWhileSubscribed(emptyList())
    val isLoadingAlbumArt = _isLoadingAlbumArt.asStateFlow()

    val uiState: StateFlow<EditAlbumUiState?> = _albumId.filterNotNull().flatMapLatest { albumId ->
        repos.album.flowAlbumCombo(albumId)
    }.filterNotNull().map { combo ->
        EditAlbumUiState(
            albumId = combo.album.albumId,
            title = combo.album.title,
            artistNames = combo.artists.map { it.name }.toImmutableList(),
            year = combo.album.year,
            artistString = combo.artists.joined(),
        )
    }.stateWhileSubscribed()

    val trackUiStates: StateFlow<ImmutableList<TrackUiState>> = _albumId.filterNotNull().map { albumId ->
        repos.track.listTrackCombosByAlbumId(albumId).map { it.toUiState() }.toImmutableList()
    }.stateWhileSubscribed(persistentListOf())

    fun cancelAlbumArtFetch(albumId: String) {
        albumArtFetchJobs.remove(albumId)?.forEach { it.cancel() }
    }

    fun clearAlbumArt(albumId: String) {
        launchOnIOThread { repos.album.clearAlbumArt(albumId) }
    }

    fun flowAlbumArt(albumId: String, context: Context): StateFlow<List<AlbumArt>> =
        MutableStateFlow<Set<AlbumArt?>>(emptySet()).also { flow ->
            _isLoadingAlbumArt.value = true

            launchOnIOThread {
                repos.album.getAlbumWithTracks(albumId)?.also { combo ->
                    combo.album.albumArt?.also { flow.value += getAlbumArt(it, context, true) }

                    val jobs = listOf(
                        launch {
                            repos.spotify.searchAlbumArt(combo)
                                .forEach { flow.value += getAlbumArt(it, context) }
                        },
                        launch {
                            combo.album.youtubePlaylist?.fullImage?.url?.toMediaStoreImage()?.also {
                                flow.value += getAlbumArt(it, context)
                            }
                        },
                        launch {
                            collectNewLocalAlbumArtUris(combo, context).forEach { uri ->
                                flow.value += getAlbumArt(uri.toMediaStoreImage(), context)
                            }
                        },
                        launch {
                            val response = repos.discogs.searchMasters(
                                query = combo.album.title,
                                artist = combo.artists.joined(),
                            )

                            response?.data?.results?.forEach { item ->
                                repos.discogs.getMaster(item.id)?.data?.images
                                    ?.filter { image -> image.type == "primary" }
                                    ?.forEach { image ->
                                        flow.value += getAlbumArt(image.uri.toMediaStoreImage(), context)
                                    }
                            }
                        },
                        launch {
                            val releaseId = combo.album.musicBrainzReleaseId ?: repos.musicBrainz.getReleaseId(combo)

                            if (releaseId != null) {
                                repos.musicBrainz.getSiblingReleaseIds(releaseId).forEach { siblingId ->
                                    repos.musicBrainz.listAllReleaseCoverArt(siblingId)?.forEach { image ->
                                        flow.value += getAlbumArt(image.image.toMediaStoreImage(), context)
                                    }
                                }
                            }
                        },
                    )

                    albumArtFetchJobs[combo.album.albumId] = jobs
                    jobs.joinAll()
                    _isLoadingAlbumArt.value = false
                    albumArtFetchJobs.remove(combo.album.albumId)
                }
            }
        }.map { it.filterNotNull() }.stateWhileSubscribed(emptyList())

    suspend fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    suspend fun listTags(albumId: String): ImmutableList<Tag> = repos.album.listTags(albumId).toImmutableList()

    fun saveAlbumArt(albumId: String, albumArt: MediaStoreImage) {
        launchOnIOThread { repos.album.updateAlbumArt(albumId, albumArt) }
    }

    fun saveAlbumArtFromUri(albumId: String, uri: Uri, context: Context, onSuccess: () -> Unit) {
        launchOnIOThread {
            try {
                val albumArt = uri.toMediaStoreImage()
                val bitmap = context.getBitmap(uri)

                assert(bitmap != null) // Just to test so it doesn't fail.
                repos.album.updateAlbumArt(albumId, albumArt)
                repos.message.onSaveAlbumArtFromUri(true)
                onSuccess()
            } catch (e: Throwable) {
                repos.message.onSaveAlbumArtFromUri(false)
            }
        }
    }

    fun setAlbumId(value: String) {
        _albumId.value = value
    }

    fun updateAlbum(
        albumId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
        tags: Collection<Tag>,
        updateMatchingTrackArtists: Boolean,
    ) {
        launchOnIOThread {
            val combo = repos.album.getAlbumWithTracks(albumId) ?: throw Exception("Album $albumId not found")
            val album = combo.album.copy(title = title, year = year)
            val albumArtists = artistNames
                .filter { it.isNotEmpty() }
                .mapIndexed { index, name ->
                    UnsavedAlbumArtistCredit(name = name, albumId = albumId, position = index)
                }

            repos.artist.setAlbumArtists(album.albumId, albumArtists)
            repos.album.upsertAlbum(album)
            repos.album.setAlbumTags(combo.album.albumId, tags)

            if (updateMatchingTrackArtists) {
                for (trackCombo in combo.trackCombos) {
                    val trackArtists =
                        if (trackCombo.trackArtists.map { it.name } == combo.artists.map { it.name }) {
                            albumArtists.map {
                                UnsavedTrackArtistCredit(
                                    name = it.name,
                                    trackId = trackCombo.track.trackId,
                                    position = it.position,
                                )
                            }.also { repos.artist.setTrackArtists(trackCombo.track.trackId, it) }
                        } else trackCombo.trackArtists

                    repos.localMedia.tagTrack(
                        track = managers.library.ensureTrackMetadata(trackCombo.track),
                        album = album,
                        trackArtists = trackArtists,
                        albumArtists = albumArtists,
                    )
                }
            }
        }
    }

    fun updateTrack(
        trackId: String,
        title: String,
        year: Int?,
        artistNames: Collection<String>,
    ) {
        launchOnIOThread {
            managers.library.updateTrack(
                trackId = trackId,
                title = title,
                year = year,
                artistNames = artistNames,
            )
        }
    }

    private fun collectNewLocalAlbumArtUris(combo: IAlbumWithTracksCombo<*, *>, context: Context): List<Uri> =
        combo.trackCombos.map { it.track }.listCoverImages(context)
            .map { it.uri }
            .filter { it != combo.album.albumArt?.fullUri }

    private suspend fun getAlbumArt(
        mediaStoreImage: MediaStoreImage,
        context: Context,
        isCurrent: Boolean = false,
    ): AlbumArt? = context.getBitmap(mediaStoreImage, 600)?.square()?.let {
        AlbumArt(mediaStoreImage, it, isCurrent)
    }
}
