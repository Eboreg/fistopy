package us.huseli.fistopy.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.plus
import us.huseli.fistopy.AlbumDownloadTask
import us.huseli.fistopy.TrackDownloadTask
import us.huseli.fistopy.dataclasses.album.AlbumCallbacks
import us.huseli.fistopy.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.fistopy.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.fistopy.dataclasses.album.ImportableAlbumUiState
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.externalcontent.ExternalListType
import us.huseli.fistopy.externalcontent.IExternalSearchBackend
import us.huseli.fistopy.externalcontent.SearchBackend
import us.huseli.fistopy.externalcontent.SearchParams
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractSearchHolder
import us.huseli.fistopy.interfaces.IExternalAlbum
import us.huseli.fistopy.interfaces.IStringIdItem
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExternalSearchViewModel @Inject constructor(
    repos: Repositories,
    private val managers: Managers,
) : AbstractAlbumListViewModel<ImportableAlbumUiState>("ExternalSearchViewModel", repos, managers) {
    private val _albumIdsInDb = mutableSetOf<String>()
    private val _backendKey = MutableStateFlow(SearchBackend.YOUTUBE)
    private val _currentBackend: IExternalSearchBackend<out IExternalAlbum>
        get() = managers.external.getSearchBackend(_backendKey.value)
    private val _currentHolder: AbstractSearchHolder<out IStringIdItem>
        get() = managers.external.getSearchBackend(_backendKey.value).getSearchHolder(_listType.value)
    private val _listType = MutableStateFlow<ExternalListType>(ExternalListType.ALBUMS)
    private val _backend = _backendKey.mapLatest { managers.external.getSearchBackend(it) }

    private val _holder: Flow<AbstractSearchHolder<out IStringIdItem>> =
        combine(_backend, _listType) { backend, listType -> backend.getSearchHolder(listType) }
    private val _albumSearchHolder: Flow<AbstractAlbumSearchHolder<out IExternalAlbum>> =
        _backend.mapLatest { it.albumSearchHolder }
    private val _trackSearchHolder: Flow<AbstractSearchHolder<TrackUiState>> =
        _backend.mapLatest { it.trackSearchHolder }

    override val baseAlbumUiStates: StateFlow<ImmutableList<ImportableAlbumUiState>> = _backend
        .flatMapLatest { it.albumSearchHolder.currentPageItems.map { states -> states.toImmutableList() } }
        .stateWhileSubscribed(persistentListOf())
    override val baseTrackUiStates: StateFlow<ImmutableList<TrackUiState>> = _trackSearchHolder
        .flatMapLatest { it.currentPageItems.map { states -> states.toImmutableList() } }
        .stateWhileSubscribed(persistentListOf())

    override val selectedAlbumIds: Flow<List<String>> = _albumSearchHolder.flatMapLatest { it.selectedItemIds }
    override val selectedTrackStateIds: StateFlow<Collection<String>> =
        _trackSearchHolder.flatMapLatest { it.selectedItemIds }.stateWhileSubscribed(emptyList())

    val backendKey = _backendKey.asStateFlow()
    val currentPage = _holder.flatMapLatest { it.currentPage }.stateWhileSubscribed(0)
    val hasNextPage = _holder.flatMapLatest { it.hasNextPage }.stateWhileSubscribed(false)
    val isEmpty = _holder.flatMapLatest { it.isEmpty }.stateWhileSubscribed(false)
    val isSearching = _holder.flatMapLatest { it.isLoadingCurrentPage }.stateWhileSubscribed(false)
    val listType = _listType.asStateFlow()
    val searchCapabilities = _holder.mapLatest { it.searchCapabilities }.stateWhileSubscribed(emptyList())

    val searchParams = combine(_listType, _backend) { listType, backend ->
        when (listType) {
            ExternalListType.ALBUMS -> backend.albumSearchHolder.searchParams
            ExternalListType.TRACKS -> backend.trackSearchHolder.searchParams
        }
    }.flattenMerge().stateWhileSubscribed(SearchParams())

    init {
        launchOnIOThread {
            _albumIdsInDb.addAll(repos.album.listAlbumIds())
        }
    }

    override fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks): AlbumSelectionCallbacks =
        super.getAlbumSelectionCallbacks(dialogCallbacks).copy(onDeleteClick = null)

    override fun onAlbumLongClick(albumId: String) = _currentBackend.albumSearchHolder.onItemLongClick(albumId)
    override fun onTrackLongClick(trackId: String) = _currentBackend.trackSearchHolder.onItemLongClick(trackId)

    override fun toggleAlbumSelected(albumId: String) = _currentBackend.albumSearchHolder.toggleItemSelected(albumId)
    override fun toggleTrackSelected(trackId: String) = _currentBackend.trackSearchHolder.toggleItemSelected(trackId)

    fun addAlbumCallbacksSaveHook(callbacks: AlbumCallbacks): AlbumCallbacks =
        callbacks.withPreHook(viewModelScope.plus(Dispatchers.IO)) { albumId ->
            if (!_albumIdsInDb.contains(albumId)) {
                val albumCombo = _currentBackend.albumSearchHolder.convertToAlbumWithTracks(albumId)

                if (albumCombo is IUnsavedAlbumCombo) {
                    managers.library.upsertAlbumWithTracks(albumCombo)
                    _albumIdsInDb.add(albumCombo.album.albumId)
                }
            }
            albumId
        }

    fun getAlbumDownloadUiStateFlow(albumId: String): StateFlow<AlbumDownloadTask.UiState?> =
        managers.library.getAlbumDownloadUiStateFlow(albumId).stateWhileSubscribed()

    fun getTrackDownloadUiStateFlow(trackId: String): StateFlow<TrackDownloadTask.UiState?> =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun gotoNextPage() = _currentHolder.gotoNextPage()
    fun gotoPreviousPage() = _currentHolder.gotoPreviousPage()

    fun initBackend() = _currentHolder.start()

    fun setBackend(value: SearchBackend) {
        _backendKey.value = value
    }

    fun setListType(value: ExternalListType) {
        _listType.value = value
    }

    fun setSearchParams(value: SearchParams) {
        _currentHolder.setSearchParams(value)
    }
}
