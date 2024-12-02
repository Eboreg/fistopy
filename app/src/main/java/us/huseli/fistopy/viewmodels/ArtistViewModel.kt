package us.huseli.fistopy.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import us.huseli.fistopy.AbstractAlbumUiStateListHandler
import us.huseli.fistopy.AbstractTrackUiStateListHandler
import us.huseli.fistopy.Constants.NAV_ARG_ARTIST
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.ListType
import us.huseli.fistopy.dataclasses.album.AlbumUiState
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.artist.ArtistUiState
import us.huseli.fistopy.dataclasses.artist.IArtist
import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroupBrowse
import us.huseli.fistopy.dataclasses.spotify.toNativeArtists
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repos: Repositories,
    savedStateHandle: SavedStateHandle,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _displayType = MutableStateFlow(DisplayType.LIST)
    private val _listType = MutableStateFlow(ListType.ALBUMS)
    private val _otherAlbumTypes =
        MutableStateFlow(listOf(AlbumType.ALBUM, AlbumType.SINGLE, AlbumType.EP, AlbumType.COMPILATION))
    private val artistId: String = savedStateHandle.get<String>(NAV_ARG_ARTIST)!!
    private val artistCombo = repos.artist.flowArtistComboById(artistId)
    private val artistMusicBrainzId = artistCombo.map { it?.artist?.musicBrainzId }.distinctUntilChanged()
    private val artistSpotifyId = artistCombo.map { it?.artist?.spotifyId }.distinctUntilChanged()

    private val albumStateHandler =
        object : AbstractAlbumUiStateListHandler<AlbumUiState>(key = "artist", repos = repos, managers = managers) {
            override val baseItems: StateFlow<List<AlbumUiState>>
                get() = repos.album.flowAlbumUiStatesByArtist(artistId).stateWhileSubscribed(emptyList())
        }

    private val musicBrainzReleaseGroups = artistMusicBrainzId
        .filterNotNull()
        .map { mbid ->
            repos.musicBrainz.flowArtistReleaseGroups(mbid)
                .toList()
                .sortedWith(MusicBrainzReleaseGroupBrowse.ReleaseGroup.comparator)
        }

    private val trackStateHandler =
        object : AbstractTrackUiStateListHandler<TrackUiState>(key = "artist", repos = repos, managers = managers) {
            override val baseItems: StateFlow<List<TrackUiState>>
                get() = repos.track.flowTrackUiStatesByArtist(artistId).stateWhileSubscribed(emptyList())
        }

    val relatedArtists: StateFlow<ImmutableList<UnsavedArtistCredit>> = artistSpotifyId
        .filterNotNull()
        .map { spotifyId ->
            repos.spotify.getRelatedArtists(spotifyId)?.toNativeArtists()?.toImmutableList() ?: persistentListOf()
        }
        .stateWhileSubscribed(persistentListOf())

    val otherAlbums: StateFlow<ImmutableList<IAlbum>> =
        combine(musicBrainzReleaseGroups, _otherAlbumTypes) { releaseGroups, albumTypes ->
            releaseGroups.filter { it.albumType in albumTypes }.map { it.toAlbum() }.toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    val otherAlbumsPreview =
        combine(musicBrainzReleaseGroups, albumStateHandler.baseItems) { releaseGroups, uiStates ->
            releaseGroups
                .filter { it.id !in uiStates.map { state -> state.musicBrainzReleaseGroupId } }
                .let { groups ->
                    val albums = groups.filter { it.albumType == AlbumType.ALBUM }
                    if (albums.size >= 10) albums else groups
                }
                .map { it.toAlbum() }
                .toImmutableList()
        }.stateWhileSubscribed(persistentListOf())

    val displayType = _displayType.asStateFlow()
    val listType = _listType.asStateFlow()
    val otherAlbumTypes: StateFlow<ImmutableList<AlbumType>> =
        _otherAlbumTypes.map { it.toImmutableList() }.stateWhileSubscribed(persistentListOf())
    val uiState: StateFlow<ArtistUiState?> = artistCombo.map { it?.toUiState() }.stateWhileSubscribed()

    val albumUiStates = albumStateHandler.items.stateWhileSubscribed(persistentListOf())
    val isLoadingAlbums = albumStateHandler.isLoadingItems
    val isLoadingTracks = trackStateHandler.isLoadingItems
    val selectedAlbumCount = albumStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val selectedTrackCount = trackStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val trackUiStates = trackStateHandler.items.stateWhileSubscribed(persistentListOf())

    fun getAlbumDownloadUiStateFlow(albumId: String) =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateWhileSubscribed()

    fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        albumStateHandler.getAlbumSelectionCallbacks(dialogCallbacks)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        trackStateHandler.getTrackSelectionCallbacks(dialogCallbacks)

    fun onAlbumClick(albumId: String, default: (String) -> Unit) = albumStateHandler.onItemClick(albumId, default)

    fun onAlbumLongClick(albumId: String) = albumStateHandler.onItemLongClick(albumId)

    fun onOtherAlbumClick(releaseGroupId: String, onGotoAlbumClick: (String) -> Unit) =
        managers.library.addTemporaryMusicBrainzAlbum(releaseGroupId, onGotoAlbumClick)

    fun onRelatedArtistClick(artist: IArtist, onGotoArtistClick: (String) -> Unit) {
        launchOnIOThread {
            val savedArtist = repos.artist.upsertArtist(artist)

            onMainThread { onGotoArtistClick(savedArtist.artistId) }

            repos.musicBrainz.matchArtist(artist)?.also { musicBrainzArtist ->
                repos.artist.upsertArtist(savedArtist.copy(musicBrainzId = musicBrainzArtist.id))
            }
        }
    }

    fun onTrackClick(state: TrackUiState) = trackStateHandler.onTrackClick(state)

    fun onTrackLongClick(trackId: String) = trackStateHandler.onItemLongClick(trackId)

    fun setDisplayType(value: DisplayType) {
        _displayType.value = value
    }

    fun setListType(value: ListType) {
        _listType.value = value
    }

    fun toggleOtherAlbumsType(albumType: AlbumType) {
        if (albumType in _otherAlbumTypes.value) _otherAlbumTypes.value -= albumType
        else _otherAlbumTypes.value += albumType
    }
}
