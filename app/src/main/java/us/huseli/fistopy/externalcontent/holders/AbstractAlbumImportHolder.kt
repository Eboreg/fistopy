package us.huseli.fistopy.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import us.huseli.fistopy.dataclasses.album.ImportableAlbumUiState
import us.huseli.fistopy.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.fistopy.interfaces.IExternalAlbum

abstract class AbstractAlbumImportHolder<T : IExternalAlbum> : AbstractImportHolder<ImportableAlbumUiState>() {
    private val _externalAlbums = mutableMapOf<String, T>()

    abstract suspend fun convertToAlbumWithTracks(externalAlbum: T, albumId: String): UnsavedAlbumWithTracksCombo?
    abstract fun getExternalAlbumChannel(): Channel<T>

    suspend fun convertToAlbumWithTracks(state: ImportableAlbumUiState): UnsavedAlbumWithTracksCombo? =
        _externalAlbums[state.id]?.let { convertToAlbumWithTracks(it, state.id) }

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
                val state = externalAlbum
                    .toAlbumCombo(isLocal = false, isInLibrary = false)
                    .toImportableUiState(playCount = externalAlbum.playCount)

                _externalAlbums[state.id] = externalAlbum
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
                set(index, get(index).copy(isSaved = true))
            }
        }
    }
}
