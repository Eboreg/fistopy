package us.huseli.fistopy.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import us.huseli.fistopy.AbstractTrackUiStateListHandler
import us.huseli.fistopy.AlbumDownloadTask
import us.huseli.fistopy.Constants.NAV_ARG_ALBUM
import us.huseli.fistopy.dataclasses.ProgressData
import us.huseli.fistopy.dataclasses.album.AlbumUiState
import us.huseli.fistopy.dataclasses.album.AlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.album.withUpdates
import us.huseli.fistopy.dataclasses.artist.Artist
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroupBrowse
import us.huseli.fistopy.dataclasses.track.AbstractTrackUiState
import us.huseli.fistopy.dataclasses.track.AlbumTrackUiState
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.TrackMergeStrategy
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.filterValuesNotNull
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
    savedStateHandle: SavedStateHandle,
) : AbstractBaseViewModel() {
    data class MusicBrainzArtistAssociation(val native: Artist, val order: Int, val musicBrainzId: String)

    data class OtherArtistAlbums(
        val albumTypes: ImmutableList<AlbumType>,
        val albums: ImmutableList<IAlbum>,
        val artist: Artist,
        val isExpanded: Boolean = false,
        val order: Int,
        val preview: ImmutableList<IAlbum>,
        val musicBrainzArtistId: String,
    )

    private val _albumId = savedStateHandle.get<String>(NAV_ARG_ALBUM)!!
    private val _albumNotFound = MutableStateFlow(false)
    private val _artists = repos.artist.flowArtistsByAlbumId(_albumId)
    private val _importProgress = MutableStateFlow(ProgressData())
    private val _otherArtistAlbumsAlbumTypes = MutableStateFlow<Map<String, List<AlbumType>>>(emptyMap())
    private val _otherArtistAlbumsExpanded = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _albumCombo: StateFlow<AlbumWithTracksCombo?> = repos.album.flowAlbumWithTracks(_albumId)
        .onEach { _albumNotFound.value = it == null }
        .filterNotNull()
        .stateWhileSubscribed()

    private val _primaryMusicBrainzArtists = combine(_albumCombo.filterNotNull(), _artists) { combo, artists ->
        if (combo.album.albumType != AlbumType.COMPILATION) {
            artists
                .associate { it.artist to it.artist.musicBrainzId }
                .filterValuesNotNull()
                .filterValues { it.isNotEmpty() }
                .toList()
                .mapIndexed { index, (artist, musicBrainzId) ->
                    MusicBrainzArtistAssociation(
                        native = artist,
                        musicBrainzId = musicBrainzId,
                        order = index,
                    )
                }
        } else emptyList()
    }

    private val _otherArtistAlbumsMap = _primaryMusicBrainzArtists.map { artists ->
        val result = mutableMapOf<MusicBrainzArtistAssociation, List<IAlbum>>()

        for (artist in artists) {
            if (result.size < 3) {
                repos.musicBrainz.flowArtistReleaseGroups(artistId = artist.musicBrainzId).toList()
                    .sortedWith(MusicBrainzReleaseGroupBrowse.ReleaseGroup.comparator)
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.toAlbum() }
                    ?.also { result[artist] = it }
            }
        }
        result
    }

    private val trackStateHandler =
        object : AbstractTrackUiStateListHandler<AlbumTrackUiState>(key = "album", repos = repos, managers = managers) {
            private val _trackCombos: StateFlow<List<TrackCombo>> =
                repos.track.flowTrackCombosByAlbumId(_albumId).stateWhileSubscribed(emptyList())

            override val baseItems: Flow<List<AlbumTrackUiState>>
                get() = combine(_albumCombo, _trackCombos) { albumCombo, trackCombos ->
                    trackCombos.map { combo ->
                        AlbumTrackUiState(
                            albumId = combo.track.albumId,
                            albumTitle = albumCombo?.album?.title,
                            artists = combo.trackArtists
                                .map { AbstractTrackUiState.Artist.fromArtistCredit(it) }
                                .toImmutableList(),
                            artistString = combo.artistString,
                            durationMs = combo.track.durationMs,
                            id = combo.track.trackId,
                            isDownloadable = combo.track.isDownloadable,
                            isInLibrary = combo.track.isInLibrary,
                            isPlayable = combo.track.isPlayable,
                            isSelected = false,
                            musicBrainzReleaseGroupId = combo.album?.musicBrainzReleaseGroupId,
                            musicBrainzReleaseId = combo.album?.musicBrainzReleaseId,
                            positionString = combo.track.getPositionString(albumCombo?.discCount ?: 1),
                            spotifyId = combo.track.spotifyId,
                            spotifyWebUrl = combo.track.spotifyWebUrl,
                            title = combo.track.title,
                            youtubeWebUrl = combo.track.youtubeWebUrl,
                            fullImageUrl = combo.fullImageUrl,
                            thumbnailUrl = combo.thumbnailUrl,
                        )
                    }
                }

            override fun playTrack(state: AbstractTrackUiState) {
                _trackCombos.value
                    .indexOfFirst { it.track.trackId == state.trackId }
                    .takeIf { it >= 0 }
                    ?.also { trackIdx -> managers.player.playAlbum(_albumId, trackIdx) }
            }
        }

    val albumNotFound = _albumNotFound.asStateFlow()
    val importProgress: StateFlow<ProgressData> = _importProgress.asStateFlow()
    val downloadState: StateFlow<AlbumDownloadTask.UiState?> =
        managers.library.getAlbumDownloadUiStateFlow(_albumId).stateWhileSubscribed()
    val selectedTrackCount = trackStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val trackUiStates = trackStateHandler.items.stateWhileSubscribed(persistentListOf())
    val uiState: StateFlow<AlbumUiState?> = _albumCombo.map { it?.toUiState() }.stateWhileSubscribed()

    val otherArtistAlbums: StateFlow<ImmutableList<OtherArtistAlbums>> = combine(
        _otherArtistAlbumsMap,
        _otherArtistAlbumsAlbumTypes,
        _otherArtistAlbumsExpanded,
    ) { albumsMap, albumTypes, expanded ->
        albumsMap.map { (artist, artistAlbums) ->
            val artistAlbumTypes = albumTypes[artist.musicBrainzId] ?: AlbumType.entries
            val filtered = artistAlbums
                .filter { artistAlbumTypes.contains(it.albumType) }
                .filter { it.id != _albumCombo.value?.album?.musicBrainzReleaseGroupId }
                .toImmutableList()
            val filteredAlbums = filtered.filter { it.albumType == AlbumType.ALBUM }.toImmutableList()

            if (filtered.isNotEmpty()) OtherArtistAlbums(
                albumTypes = artistAlbumTypes.toImmutableList(),
                albums = filtered,
                artist = artist.native,
                isExpanded = expanded[artist.musicBrainzId] ?: false,
                order = artist.order,
                preview = if (filteredAlbums.size >= 10) filteredAlbums else filtered,
                musicBrainzArtistId = artist.musicBrainzId,
            ) else null
        }.filterNotNull().sortedBy { it.order }.toImmutableList()
    }.stateWhileSubscribed(persistentListOf())

    val positionColumnWidthDp: StateFlow<Int> = trackStateHandler.baseItems.map { states ->
        val trackPositions = states.map { it.positionString }
        trackPositions.maxOfOrNull { it.length * 10 }?.plus(10) ?: 40
    }.stateWhileSubscribed(40)

    val tagNames: StateFlow<ImmutableList<String>> = repos.album.flowTagNamesByAlbumId(_albumId)
        .map { it.toImmutableList() }
        .stateWhileSubscribed(persistentListOf())

    init {
        trackStateHandler.unselectAllItems()
        refetchIfNeeded()
    }

    fun ensureTrackMetadataAsync(trackId: String) = managers.library.ensureTrackMetadataAsync(trackId)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        trackStateHandler.getTrackSelectionCallbacks(dialogCallbacks)

    fun matchUnplayableTracks() {
        launchOnMainThread {
            _albumCombo.value?.also { combo ->
                managers.library.matchUnplayableTracks(combo).collect { _importProgress.value = it }
            }
        }
    }

    fun onOtherArtistAlbumClick(externalId: String, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporaryMusicBrainzAlbum(externalId, onGotoAlbumClick)

    fun onTrackClick(state: AlbumTrackUiState) = trackStateHandler.onTrackClick(state)

    fun onTrackLongClick(trackId: String) = trackStateHandler.onItemLongClick(trackId)

    fun toggleOtherArtistAlbumsAlbumType(obj: OtherArtistAlbums, albumType: AlbumType) {
        val current = obj.albumTypes.toMutableList()

        if (current.contains(albumType)) current -= albumType
        else current += albumType
        _otherArtistAlbumsAlbumTypes.value += obj.musicBrainzArtistId to current
    }

    fun toggleOtherArtistAlbumsExpanded(obj: OtherArtistAlbums) {
        _otherArtistAlbumsExpanded.value += obj.musicBrainzArtistId to !obj.isExpanded
    }

    private fun refetchIfNeeded() = launchOnIOThread {
        _albumCombo.transformWhile {
            if (it != null) emit(it)
            it == null
        }.collect { combo ->
            if (combo.album.trackCount != null && combo.album.trackCount > combo.tracks.size) {
                val newCombo = combo.album.spotifyId?.let { spotifyId ->
                    repos.spotify
                        .getAlbum(spotifyId)
                        ?.toAlbumWithTracks(
                            isLocal = combo.album.isLocal,
                            isInLibrary = combo.album.isInLibrary,
                            albumId = combo.album.albumId,
                        )
                } ?: combo.album.musicBrainzReleaseId?.let { musicBrainzId ->
                    repos.musicBrainz
                        .getRelease(musicBrainzId)
                        ?.toAlbumWithTracks(
                            isLocal = combo.album.isLocal,
                            isInLibrary = combo.album.isInLibrary,
                            albumId = combo.album.albumId,
                        )
                }

                if (newCombo != null) {
                    managers.library.upsertAlbumWithTracks(
                        combo.withUpdates {
                            mergeAlbum(newCombo.album)
                            mergeTrackCombos(
                                other = newCombo.trackCombos,
                                mergeStrategy = TrackMergeStrategy.KEEP_MOST,
                            )
                            mergeTags(other = newCombo.tags)
                            mergeArtists(other = newCombo.artists, updateStrategy = ListUpdateStrategy.MERGE)
                        }
                    )
                }
            }
        }
    }
}
