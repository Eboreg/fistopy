package us.huseli.fistopy.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.album.DeleteAlbumsDialog
import us.huseli.fistopy.compose.album.EditAlbumMethodDialog
import us.huseli.fistopy.compose.export.ExportTracksDialog
import us.huseli.fistopy.compose.imports.PostImportDialog
import us.huseli.fistopy.compose.playlist.AddTracksToPlaylistDialog
import us.huseli.fistopy.compose.playlist.CreatePlaylistDialog
import us.huseli.fistopy.compose.settings.LocalMusicUriDialog
import us.huseli.fistopy.compose.track.EditTrackDialog
import us.huseli.fistopy.compose.track.TrackInfoDialog
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.RootStateViewModel

@Composable
inline fun rememberDialogCallbacks(
    crossinline onGotoAlbumClick: @DisallowComposableCalls (String) -> Unit,
    crossinline onGotoPlaylistClick: @DisallowComposableCalls (String) -> Unit,
    crossinline onGotoLibraryClick: () -> Unit,
    viewModel: RootStateViewModel = hiltViewModel(),
): AppDialogCallbacks {
    val addToPlaylistTrackIds by viewModel.addToPlaylistTrackIds.collectAsStateWithLifecycle()
    val albumImportData by viewModel.albumImportData.collectAsStateWithLifecycle()
    val albumToDownload by viewModel.albumToDownload.collectAsStateWithLifecycle()
    val deleteAlbumIds by viewModel.deleteAlbums.collectAsStateWithLifecycle()
    val editAlbumId by viewModel.editAlbumId.collectAsStateWithLifecycle()
    val editTrackState by viewModel.editTrackState.collectAsStateWithLifecycle()
    val exportAlbumIds by viewModel.exportAlbumIds.collectAsStateWithLifecycle()
    val exportPlaylistId by viewModel.exportPlaylistId.collectAsStateWithLifecycle()
    val exportTrackIds by viewModel.exportTrackIds.collectAsStateWithLifecycle()
    val localMusicUri by viewModel.localMusicUri.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()
    val showInfoTrackCombo by viewModel.showInfoTrackCombo.collectAsStateWithLifecycle()
    val showLibraryRadioDialog by viewModel.showLibraryRadioDialog.collectAsStateWithLifecycle()

    albumImportData?.also { data ->
        PostImportDialog(
            data = data,
            onDismissRequest = { viewModel.albumImportData.value = null },
            onGotoAlbumClick = { onGotoAlbumClick(it) },
            onGotoLibraryClick = { onGotoLibraryClick() },
        )
    }

    if (exportTrackIds.isNotEmpty() || exportAlbumIds.isNotEmpty() || exportPlaylistId != null) {
        ExportTracksDialog(
            trackIds = exportTrackIds,
            albumIds = exportAlbumIds,
            playlistId = exportPlaylistId,
            onClose = { viewModel.clearExports() },
        )
    }

    addToPlaylistTrackIds.takeIf { it.isNotEmpty() }?.also { trackIds ->
        AddTracksToPlaylistDialog(
            trackIds = trackIds.toImmutableList(),
            onPlaylistClick = { onGotoPlaylistClick(it) },
            onClose = { viewModel.setAddToPlaylistTrackIds(emptyList()) },
        )
    }

    albumToDownload?.also { album ->
        if (localMusicUri == null) {
            LocalMusicUriDialog(
                onSave = { viewModel.setLocalMusicUri(it) },
                text = { Text(stringResource(R.string.you_need_to_select_your_local_music_root_folder)) },
            )
        } else {
            viewModel.setAlbumToDownloadId(null)
            viewModel.downloadAlbum(album = album, onGotoAlbumClick = { onGotoAlbumClick(album.albumId) })
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onSave = { name ->
                viewModel.createPlaylist(name = name, onFinish = { onGotoPlaylistClick(it) })
                viewModel.showCreatePlaylistDialog.value = false
            },
            onCancel = { viewModel.showCreatePlaylistDialog.value = false },
        )
    }

    deleteAlbumIds.takeIf { it.isNotEmpty() }?.also { albumIds ->
        DeleteAlbumsDialog(albumIds = albumIds, onClose = { viewModel.deleteAlbums.value = persistentListOf() })
    }

    editAlbumId?.also { EditAlbumMethodDialog(albumId = it, onClose = { viewModel.editAlbumId.value = null }) }

    editTrackState?.also { EditTrackDialog(state = it, onClose = { viewModel.setEditTrackId(null) }) }

    showInfoTrackCombo?.also {
        TrackInfoDialog(trackCombo = it, onClose = { viewModel.setShowInfoTrackId(null) })
    }

    if (showLibraryRadioDialog) RadioDialog(onDismissRequest = { viewModel.showLibraryRadioDialog.value = false })

    return remember {
        AppDialogCallbacks(
            onAddAlbumsToPlaylistClick = { viewModel.setAddToPlaylistAlbumIds(it) },
            onAddArtistToPlaylistClick = { viewModel.setAddToPlaylistArtistId(it) },
            onAddTracksToPlaylistClick = { viewModel.setAddToPlaylistTrackIds(it) },
            onCreatePlaylistClick = { viewModel.showCreatePlaylistDialog.value = true },
            onDeleteAlbumsClick = { viewModel.deleteAlbums.value = it.toImmutableList() },
            onDownloadAlbumClick = { viewModel.setAlbumToDownloadId(it) },
            onEditAlbumClick = { viewModel.editAlbumId.value = it },
            onEditTrackClick = { viewModel.setEditTrackId(it) },
            onExportAlbumsClick = { viewModel.setExportAlbumIds(it) },
            onExportAllTracksClick = { viewModel.setExportAllTracks() },
            onExportPlaylistClick = { viewModel.setExportPlaylistId(it) },
            onExportTracksClick = { viewModel.setExportTrackIds(it) },
            onRadioClick = { viewModel.showLibraryRadioDialog.value = true },
            onShowTrackInfoClick = { viewModel.setShowInfoTrackId(it) },
        )
    }
}
