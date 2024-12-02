package us.huseli.fistopy.externalcontent

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.fistopy.repositories.Repositories

class LocalBackend(
    private val repos: Repositories,
    private val context: Context,
) : IExternalImportBackend {
    private val _localImportUri = MutableStateFlow<Uri?>(null)

    override val albumImportHolder = object : AbstractAlbumImportHolder<UnsavedAlbum>() {
        private val _importDirectoryFile =
            _localImportUri.map { uri -> uri?.let { DocumentFile.fromTreeUri(context, it) } }

        override val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
        override val isTotalCountExact: Flow<Boolean> = flowOf(true)
        override val canImport = _importDirectoryFile.map { it != null }

        override fun getExternalAlbumChannel() =
            Channel<ExternalAlbumWithTracksCombo<UnsavedAlbum>>().also { channel ->
                launchOnIOThread {
                    _importDirectoryFile.filterNotNull().collectLatest { documentFile ->
                        val existingAlbumCombos = repos.album.listAlbumCombos()
                        val existingTrackUris = repos.track.listTrackLocalUris()
                        val albumChannel = repos.localMedia.importableAlbumsChannel(
                            treeDocumentFile = documentFile,
                            existingTrackUris = existingTrackUris,
                            existingAlbumCombos = existingAlbumCombos,
                        )

                        _items.value = emptyList()
                        _allItemsFetched.value = false
                        for (combo in albumChannel) {
                            channel.send(combo)
                        }
                        _allItemsFetched.value = true
                    }
                }
            }

        override suspend fun getPreviouslyImportedIds(): List<String> = emptyList()
    }

    fun setLocalImportUri(uri: Uri) {
        _localImportUri.value = uri
    }
}
