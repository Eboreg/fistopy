package us.huseli.fistopy

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import us.huseli.fistopy.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.fistopy.dataclasses.album.IAlbumUiState
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.track.AbstractTrackUiState
import us.huseli.fistopy.dataclasses.track.ISelectableTrackUiState
import us.huseli.fistopy.dataclasses.track.TrackSelectionCallbacks
import us.huseli.fistopy.interfaces.ISelectableItem
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories

abstract class AbstractSelectableItemListHandler<out T : ISelectableItem>(
    private val key: String,
    private val repos: Repositories,
) : AbstractScopeHolder() {
    private val _isLoadingItems = MutableStateFlow(true)
    private val _selectedItemIds = repos.itemSelection.getSelectedItemIds(key)

    abstract val baseItems: Flow<List<T>>

    open val selectedItemIds: Flow<List<String>>
        get() = combine(_selectedItemIds, baseItems) { selectedIds, items ->
            selectedIds.filter { items.map { item -> item.id }.contains(it) }
        }

    val isLoadingItems = _isLoadingItems.asStateFlow()

    val selectedItemCount: Flow<Int>
        get() = selectedItemIds.map { it.size }

    @Suppress("UNCHECKED_CAST")
    val items: Flow<ImmutableList<T>>
        get() = combine(baseItems, selectedItemIds) { items, selectedIds ->
            items
                .map { item -> item.withIsSelected(selectedIds.contains(item.id)) as T }
                .toImmutableList()
        }.onEach { _isLoadingItems.value = false }

    open fun onItemClick(itemId: String, default: (String) -> Unit) {
        launchOnMainThread {
            if (isItemSelectEnabled()) toggleItemSelected(itemId)
            else default(itemId)
        }
    }

    open fun onItemLongClick(itemId: String) {
        launchOnMainThread {
            val itemIds = selectedItemIds.first().lastOrNull()?.let { id ->
                baseItems.first()
                    .map { it.id }
                    .listItemsBetween(item1 = id, item2 = itemId)
                    .plus(itemId)
            } ?: listOf(itemId)

            setItemsIsSelected(itemIds, true)
        }
    }

    open fun setItemsIsSelected(itemIds: Iterable<String>, value: Boolean) {
        repos.itemSelection.setItemsIsSelected(key, itemIds, value)
    }

    open fun unselectAllItems() {
        repos.itemSelection.unselectAllItems(key)
    }

    suspend fun getSortedSelectedItems(): List<T> {
        val allItems = baseItems.first()
        val selectedItems = allItems.filter { selectedItemIds.first().contains(it.id) }

        return selectedItems.sortedLike(allItems, key = { it.id })
    }

    fun selectAllItems() {
        launchOnMainThread {
            setItemsIsSelected(baseItems.first().map { it.id }, true)
        }
    }

    fun toggleItemSelected(itemId: String) {
        launchOnMainThread {
            setItemsIsSelected(
                itemIds = listOf(itemId),
                value = !isItemSelected(itemId),
            )
        }
    }

    fun withSelectedItemIds(callback: (List<String>) -> Unit) {
        launchOnMainThread { callback(selectedItemIds.first()) }
    }

    private suspend fun isItemSelected(itemId: String): Boolean = selectedItemIds.first().contains(itemId)

    private suspend fun isItemSelectEnabled(): Boolean = selectedItemIds.first().isNotEmpty()
}


abstract class AbstractAlbumUiStateListHandler<T : IAlbumUiState>(
    key: String,
    repos: Repositories,
    private val managers: Managers,
) : AbstractSelectableItemListHandler<T>("$key.albums", repos) {
    open fun getAlbumSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) = AlbumSelectionCallbacks(
        onAddToPlaylistClick = { withSelectedItemIds(dialogCallbacks.onAddAlbumsToPlaylistClick) },
        onDeleteClick = { withSelectedItemIds(dialogCallbacks.onDeleteAlbumsClick) },
        onEnqueueClick = { withSelectedItemIds { managers.player.enqueueAlbums(it) } },
        onExportClick = { withSelectedItemIds(dialogCallbacks.onExportAlbumsClick) },
        onPlayClick = { withSelectedItemIds { managers.player.playAlbums(it) } },
        onSelectAllClick = { launchOnMainThread { setItemsIsSelected(baseItems.first().map { it.id }, true) } },
        onUnselectAllClick = { unselectAllItems() },
    )
}


abstract class AbstractTrackUiStateListHandler<out T : ISelectableTrackUiState>(
    key: String,
    repos: Repositories,
    private val managers: Managers,
) : AbstractSelectableItemListHandler<T>("$key.tracks", repos) {
    open fun enqueueSelectedTracks() {
        launchOnMainThread { managers.player.enqueueTracks(getSortedSelectedItems().map { it.trackId }) }
    }

    open fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) = TrackSelectionCallbacks(
        onAddToPlaylistClick = { withSelectedItemIds(dialogCallbacks.onAddTracksToPlaylistClick) },
        onEnqueueClick = { enqueueSelectedTracks() },
        onExportClick = { withSelectedItemIds(dialogCallbacks.onExportTracksClick) },
        onPlayClick = { playSelectedTracks() },
        onSelectAllClick = { selectAllItems() },
        onUnselectAllClick = { unselectAllItems() },
    )

    fun onTrackClick(state: AbstractTrackUiState) {
        onItemClick(
            itemId = state.id,
            default = { playTrack(state) },
        )
    }

    open fun playSelectedTracks() {
        launchOnMainThread { managers.player.playTracks(getSortedSelectedItems().map { it.trackId }) }
    }

    open fun playTrack(state: AbstractTrackUiState) {
        if (state.isPlayable) managers.player.playTrack(state.trackId)
    }
}
