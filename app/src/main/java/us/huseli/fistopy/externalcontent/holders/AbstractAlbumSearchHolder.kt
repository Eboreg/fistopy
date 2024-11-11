package us.huseli.fistopy.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.ImportableAlbumUiState
import us.huseli.fistopy.externalcontent.SearchParams
import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractAlbumSearchHolder<T : IStringIdItem> : AbstractSearchHolder<ImportableAlbumUiState>() {
    private var _existingAlbumCombos: Map<String, AlbumCombo>? = null

    protected val externalAlbums = mutableMapOf<String, ExternalAlbumWithTracksCombo<T>>()

    protected abstract fun getExternalAlbumChannel(searchParams: SearchParams): Channel<ExternalAlbumWithTracksCombo<T>>

    protected abstract suspend fun loadExistingAlbumCombos(): Map<String, AlbumCombo>

    abstract suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<*>?

    override fun getResultChannel(searchParams: SearchParams) = Channel<ImportableAlbumUiState>().also { channel ->
        launchOnIOThread {
            for (externalAlbum in getExternalAlbumChannel(searchParams)) {
                val combo = getExistingAlbumCombos()[externalAlbum.id] ?: externalAlbum

                externalAlbums[combo.id] = externalAlbum
                channel.send(combo.toImportableUiState().copy(playCount = externalAlbum.playCount))
            }
            channel.close()
        }
    }

    private suspend fun getExistingAlbumCombos(): Map<String, AlbumCombo> {
        _existingAlbumCombos?.also { return it }
        return loadExistingAlbumCombos().also { _existingAlbumCombos = it }
    }
}
