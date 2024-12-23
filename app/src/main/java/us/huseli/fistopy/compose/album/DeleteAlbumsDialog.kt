package us.huseli.fistopy.compose.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.FistopyTheme
import us.huseli.fistopy.compose.album.DeleteAlbumsAction.DELETE_ALBUMS_AND_FILES
import us.huseli.fistopy.compose.album.DeleteAlbumsAction.DELETE_FILES_KEEP_ALBUM
import us.huseli.fistopy.compose.album.DeleteAlbumsAction.HIDE_ALBUMS_KEEP_FILES
import us.huseli.fistopy.compose.utils.CancelButton
import us.huseli.fistopy.compose.utils.SaveButton
import us.huseli.fistopy.dataclasses.album.LocalAlbumCallbacks
import us.huseli.fistopy.dataclasses.callbacks.LocalAppCallbacks
import us.huseli.fistopy.pluralStringResource
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.DeleteAlbumsViewModel

enum class DeleteAlbumsAction { HIDE_ALBUMS_KEEP_FILES, DELETE_ALBUMS_AND_FILES, DELETE_FILES_KEEP_ALBUM }

@Composable
fun DeleteAlbumsDialog(
    albumIds: ImmutableCollection<String>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAlbumsViewModel = hiltViewModel(),
) {
    val callbacks = LocalAlbumCallbacks.current
    val isImportingLocalMedia by viewModel.isImportingLocalMedia.collectAsStateWithLifecycle()
    val states by viewModel.albumUiStates.collectAsStateWithLifecycle()
    val appCallbacks = LocalAppCallbacks.current

    LaunchedEffect(albumIds) {
        viewModel.setAlbumIds(albumIds)
    }

    if (states.isNotEmpty()) {
        if (isImportingLocalMedia) {
            AlertDialog(
                onDismissRequest = onClose,
                confirmButton = { CancelButton(text = stringResource(R.string.close), onClick = onClose) },
                title = { Text(pluralStringResource(R.plurals.delete_x_albums, states.size, states.size)) },
                text = { Text(stringResource(R.string.cannot_delete_albums_while_auto_import)) },
            )
        } else if (states.all { !it.isLocal && !it.isPartiallyDownloaded }) {
            viewModel.hideAlbums(
                onGotoLibraryClick = appCallbacks.onGotoLibraryClick,
                onGotoAlbumClick = callbacks.onGotoAlbumClick,
            )
            onClose()
        } else {
            DeleteAlbumsDialog(
                count = states.size,
                onCancel = onClose,
                onDeleteClick = { action ->
                    if (action == HIDE_ALBUMS_KEEP_FILES || action == DELETE_ALBUMS_AND_FILES) {
                        viewModel.hideAlbums(
                            deleteFiles = action == DELETE_ALBUMS_AND_FILES,
                            onGotoLibraryClick = appCallbacks.onGotoLibraryClick,
                            onGotoAlbumClick = callbacks.onGotoAlbumClick,
                        )
                    } else if (action == DELETE_FILES_KEEP_ALBUM) {
                        viewModel.deleteLocalAlbumFiles()
                    }
                    onClose()
                },
                modifier = modifier,
            )
        }
    }
}


@Composable
fun DeleteAlbumsDialog(
    count: Int,
    onCancel: () -> Unit,
    onDeleteClick: (DeleteAlbumsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        dismissButton = { CancelButton(onClick = onCancel) },
        title = { Text(pluralStringResource(R.plurals.delete_x_albums, count, count)) },
        confirmButton = {
            SaveButton(
                text = pluralStringResource(R.plurals.remove_album_and_delete_files, count, count),
                onClick = { onDeleteClick(DELETE_ALBUMS_AND_FILES) },
            )
            SaveButton(
                text = pluralStringResource(R.plurals.hide_album_keep_local_files, count, count),
                onClick = { onDeleteClick(HIDE_ALBUMS_KEEP_FILES) },
            )
            SaveButton(
                text = pluralStringResource(R.plurals.delete_local_files_keep_album, count, count),
                onClick = { onDeleteClick(DELETE_FILES_KEEP_ALBUM) },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.what_do_you_want_to_do))
                Text(
                    text = stringResource(R.string.may_not_be_able_to_delete_local_files),
                    style = FistopyTheme.typography.bodySmall,
                )
            }
        },
    )
}
