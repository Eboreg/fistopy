package us.huseli.thoucylinder.compose.album

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.PlaylistAdd
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.sharp.BookmarkBorder
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Download
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.dataclasses.abstr.AbstractArtist
import us.huseli.thoucylinder.dataclasses.callbacks.AlbumCallbacks
import us.huseli.thoucylinder.stringResource

@Composable
fun AlbumContextMenu(
    albumId: String,
    albumArtists: ImmutableCollection<AbstractArtist>,
    isLocal: Boolean,
    isInLibrary: Boolean,
    isPartiallyDownloaded: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    callbacks: AlbumCallbacks,
    spotifyWebUrl: String? = null,
    youtubeWebUrl: String? = null,
) {
    val uriHandler = LocalUriHandler.current

    DropdownMenu(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        expanded = expanded,
    ) {
        callbacks.onPlayClick?.also { onPlayClick ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.play)) },
                leadingIcon = { Icon(Icons.Sharp.PlayArrow, null) },
                onClick = {
                    onPlayClick(albumId)
                    onDismissRequest()
                }
            )
        }

        callbacks.onEnqueueClick?.also { onEnqueueClick ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.enqueue)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistPlay, null) },
                onClick = {
                    onEnqueueClick(albumId)
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = { Text(stringResource(R.string.start_radio)) },
            leadingIcon = { Icon(Icons.Sharp.Radio, null) },
            onClick = {
                callbacks.onStartRadioClick(albumId)
                onDismissRequest()
            }
        )

        if (!isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_library)) },
            leadingIcon = { Icon(Icons.Sharp.BookmarkBorder, null) },
            onClick = {
                callbacks.onAddToLibraryClick(albumId)
                onDismissRequest()
            },
        )

        if (!isLocal || isPartiallyDownloaded) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.download)) },
            leadingIcon = { Icon(Icons.Sharp.Download, null) },
            onClick = {
                callbacks.onDownloadClick(albumId)
                onDismissRequest()
            },
        )

        if (isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.edit)) },
            leadingIcon = { Icon(Icons.Sharp.Edit, null) },
            onClick = {
                callbacks.onEditClick(albumId)
                onDismissRequest()
            },
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_to_playlist)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Sharp.PlaylistAdd, null) },
            onClick = {
                callbacks.onAddToPlaylistClick(albumId)
                onDismissRequest()
            }
        )

        albumArtists.forEach { albumArtist ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.go_to_x, albumArtist.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Sharp.InterpreterMode, null) },
                onClick = {
                    callbacks.onArtistClick(albumArtist.artistId)
                    onDismissRequest()
                },
            )
        }

        youtubeWebUrl?.also {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_youtube)) },
                leadingIcon = { Icon(painterResource(R.drawable.youtube), null) },
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                }
            )
        }

        spotifyWebUrl?.also {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.play_on_spotify)) },
                leadingIcon = { Icon(painterResource(R.drawable.spotify), null) },
                onClick = {
                    uriHandler.openUri(it)
                    onDismissRequest()
                }
            )
        }

        if (isInLibrary) DropdownMenuItem(
            text = { Text(text = stringResource(R.string.delete_album)) },
            leadingIcon = { Icon(Icons.Sharp.Delete, null) },
            onClick = {
                callbacks.onDeleteClick(albumId)
                onDismissRequest()
            }
        )
    }
}


@Composable
fun AlbumContextMenuWithButton(
    albumId: String,
    albumArtists: ImmutableCollection<AbstractArtist>,
    isLocal: Boolean,
    isInLibrary: Boolean,
    isPartiallyDownloaded: Boolean,
    modifier: Modifier = Modifier,
    callbacks: AlbumCallbacks,
    buttonIconSize: Dp = 30.dp,
    spotifyWebUrl: String? = null,
    youtubeWebUrl: String? = null,
) {
    var isMenuShown by rememberSaveable { mutableStateOf(false) }

    IconButton(
        modifier = modifier.size(32.dp, 40.dp),
        onClick = { isMenuShown = !isMenuShown },
        content = {
            AlbumContextMenu(
                albumId = albumId,
                albumArtists = albumArtists,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                expanded = isMenuShown,
                onDismissRequest = { isMenuShown = false },
                callbacks = callbacks,
                isPartiallyDownloaded = isPartiallyDownloaded,
                youtubeWebUrl = youtubeWebUrl,
                spotifyWebUrl = spotifyWebUrl,
            )
            Icon(Icons.Sharp.MoreVert, null, modifier = Modifier.size(buttonIconSize))
        }
    )
}
