package us.huseli.fistopy.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractAlbumUiStateListHandler
import us.huseli.fistopy.AbstractTrackUiStateListHandler
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.ListType
import us.huseli.fistopy.dataclasses.album.AlbumUiState
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.tag.TagPojo
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.enums.AlbumSortParameter
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.SortOrder
import us.huseli.fistopy.enums.TrackSortParameter
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val albumStateHandler =
        object : AbstractAlbumUiStateListHandler<AlbumUiState>(key = "library", repos = repos, managers = managers) {
            override val baseItems: Flow<List<AlbumUiState>>
                get() = combine(
                    repos.settings.albumSortParameter,
                    repos.settings.albumSortOrder,
                    repos.settings.albumSearchTerm,
                    repos.settings.libraryAlbumTagFilter,
                    repos.settings.libraryAvailabilityFilter,
                ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
                    repos.album.flowAlbumUiStates(
                        sortParameter = sortParameter,
                        sortOrder = sortOrder,
                        searchTerm = searchTerm,
                        tagNames = tagPojos.map { it.name },
                        availabilityFilter = availability,
                    )
                }.flattenMerge()
        }

    private val trackStateHandler =
        object : AbstractTrackUiStateListHandler<TrackUiState>(key = "library", repos = repos, managers = managers) {
            override val baseItems: Flow<List<TrackUiState>>
                get() = combine(
                    repos.settings.trackSortParameter,
                    repos.settings.trackSortOrder,
                    repos.settings.trackSearchTerm,
                    repos.settings.libraryTrackTagFilter,
                    repos.settings.libraryAvailabilityFilter,
                ) { sortParameter, sortOrder, searchTerm, tagPojos, availability ->
                    repos.track.flowTrackUiStates(
                        sortParameter = sortParameter,
                        sortOrder = sortOrder,
                        searchTerm = searchTerm,
                        tagNames = tagPojos.map { it.name },
                        availabilityFilter = availability,
                    )
                }.flattenMerge()
        }

    val albumSearchTerm = repos.settings.albumSearchTerm
    val albumSortOrder = repos.settings.albumSortOrder
    val albumUiStates = albumStateHandler.items.stateWhileSubscribed(persistentListOf())
    val albumSortParameter = repos.settings.albumSortParameter
    val availabilityFilter = repos.settings.libraryAvailabilityFilter
    val displayType = repos.settings.libraryDisplayType
    val isImportingLocalMedia = repos.localMedia.isImportingLocalMedia
    val isLoadingAlbums = albumStateHandler.isLoadingItems
    val isLoadingTracks = trackStateHandler.isLoadingItems
    val isLocalMediaDirConfigured: StateFlow<Boolean> =
        repos.settings.localMusicUri.map { it != null }.stateWhileSubscribed(false)
    val listType = repos.settings.libraryListType
    val selectedAlbumCount = albumStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val selectedAlbumTagPojos = repos.settings.libraryAlbumTagFilter
    val selectedTrackCount = trackStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val selectedTrackTagPojos = repos.settings.libraryTrackTagFilter
    val trackSearchTerm = repos.settings.trackSearchTerm
    val trackSortOrder = repos.settings.trackSortOrder
    val trackSortParameter = repos.settings.trackSortParameter
    val trackUiStates = trackStateHandler.items.stateWhileSubscribed(persistentListOf())

    val albumTagPojos = repos.settings.libraryAvailabilityFilter.flatMapLatest { repos.album.flowTagPojos(it) }
        .map { it.toImmutableList() }
        .stateWhileSubscribed(persistentListOf())

    val trackTagPojos = repos.settings.libraryAvailabilityFilter.flatMapLatest { repos.track.flowTagPojos(it) }
        .map { it.toImmutableList() }
        .stateWhileSubscribed(persistentListOf())

    val isAlbumsEmpty: StateFlow<Boolean> = combine(
        albumStateHandler.baseItems,
        albumStateHandler.isLoadingItems,
        repos.localMedia.isImportingLocalMedia,
    ) { states, isLoading, isImporting ->
        states.isEmpty() && !isLoading && !isImporting
    }.stateWhileSubscribed(false)

    val isTracksEmpty: StateFlow<Boolean> = combine(
        trackStateHandler.baseItems,
        trackStateHandler.isLoadingItems,
        repos.localMedia.isImportingLocalMedia,
    ) { states, isLoading, isImporting ->
        states.isEmpty() && !isLoading && !isImporting
    }.stateWhileSubscribed(false)

    fun getAlbumDownloadUiStateFlow(albumId: String) =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateWhileSubscribed()

    fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        albumStateHandler.getAlbumSelectionCallbacks(dialogCallbacks)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        trackStateHandler.getTrackSelectionCallbacks(dialogCallbacks)

    fun importNewLocalAlbums() {
        launchOnIOThread { managers.library.importNewLocalAlbums() }
    }

    fun onAlbumClick(albumId: String, default: (String) -> Unit) =
        albumStateHandler.onItemClick(albumId, default)

    fun onAlbumLongClick(albumId: String) = albumStateHandler.onItemLongClick(albumId)

    fun onTrackClick(state: TrackUiState) = trackStateHandler.onTrackClick(state)

    fun onTrackLongClick(trackId: String) = trackStateHandler.onItemLongClick(trackId)

    fun setAlbumSearchTerm(value: String) = repos.settings.setAlbumSearchTerm(value)

    fun setAlbumSorting(sortParameter: AlbumSortParameter, sortOrder: SortOrder) =
        repos.settings.setAlbumSorting(sortParameter, sortOrder)

    fun setAvailabilityFilter(value: AvailabilityFilter) = repos.settings.setLibraryAvailabilityFilter(value)

    fun setDisplayType(value: DisplayType) = repos.settings.setLibraryDisplayType(value)

    fun setListType(value: ListType) = repos.settings.setLibraryListType(value)

    fun setSelectedAlbumTagPojos(value: List<TagPojo>) = repos.settings.setLibraryAlbumTagFilter(value)

    fun setSelectedTrackTagPojos(value: List<TagPojo>) = repos.settings.setLibraryTrackTagFilter(value)

    fun setTrackSearchTerm(value: String) = repos.settings.setTrackSearchTerm(value)

    fun setTrackSorting(sortParameter: TrackSortParameter, sortOrder: SortOrder) =
        repos.settings.setTrackSorting(sortParameter, sortOrder)
}
