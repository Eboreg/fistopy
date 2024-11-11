package us.huseli.fistopy.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.ImportableAlbumUiState
import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractAlbumImportHolder<T : IStringIdItem> : AbstractImportHolder<ImportableAlbumUiState>() {
    protected val externalAlbums = mutableMapOf<String, ExternalAlbumWithTracksCombo<T>>()

    abstract fun getExternalAlbumChannel(): Channel<ExternalAlbumWithTracksCombo<T>>

    open suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<*>? = externalAlbums[albumId]

    fun updateItemId(oldId: String, newId: String) {
        if (_selectedItemIds.value.contains(oldId)) {
            _selectedItemIds.value = _selectedItemIds.value.toMutableList().apply {
                remove(oldId)
                add(newId)
            }
        }
        _items.value.indexOfFirst { it.id == oldId }.takeIf { it >= 0 }?.also { index ->
            _items.value = _items.value.toMutableList().apply {
                set(index, get(index).copy(albumId = newId))
            }
        }
    }

    override fun getResultChannel() = Channel<ImportableAlbumUiState>().also { channel ->
        launchOnIOThread {
            for (externalAlbum in getExternalAlbumChannel()) {
                val state = externalAlbum.toImportableUiState()

                externalAlbums[state.id] = externalAlbum
                channel.send(state)
            }
        }
    }

    override fun itemMatchesSearchTerm(item: ImportableAlbumUiState, term: String): Boolean =
        item.matchesSearchTerm(term)

    override fun onItemImportError(itemId: String, error: String) {
        _selectedItemIds.value -= itemId
        _items.value.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }?.also { index ->
            _items.value = _items.value.toMutableList().apply {
                set(index, get(index).copy(importError = error))
            }
        }
    }

    override fun onItemImportFinished(itemId: String) {
        _selectedItemIds.value -= itemId
        _items.value.indexOfFirst { it.id == itemId }.takeIf { it >= 0 }?.also { index ->
            _items.value = _items.value.toMutableList().apply {
                set(index, get(index).copy(isImported = true))
            }
        }
    }
}
