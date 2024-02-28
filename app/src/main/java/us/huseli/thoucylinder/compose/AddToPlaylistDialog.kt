package us.huseli.thoucylinder.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.pluralStringResource
import us.huseli.thoucylinder.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.ItemList
import us.huseli.thoucylinder.dataclasses.pojos.PlaylistPojo
import us.huseli.thoucylinder.umlautify

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistPojo>,
    onSelect: (PlaylistPojo) -> Unit,
    onCreateNewClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(onDismissRequest = onCancel, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.add_to_playlist), style = MaterialTheme.typography.headlineSmall)

                ItemList(
                    things = playlists,
                    cardHeight = 50.dp,
                    onClick = { _, pojo -> onSelect(pojo) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    key = { _, pojo -> pojo.playlistId },
                ) { _, playlist ->
                    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = playlist.name.umlautify(),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = " • " + pluralStringResource(
                                    R.plurals.x_tracks,
                                    playlist.trackCount,
                                    playlist.trackCount,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                if (playlists.isEmpty()) Text(text = stringResource(R.string.no_playlists_found))

                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        content = { Text(stringResource(R.string.cancel)) },
                    )
                    TextButton(onClick = onCreateNewClick) { Text(stringResource(R.string.create_new_playlist)) }
                }
            }
        }
    }
}


@Composable
fun AddDuplicatesToPlaylistDialog(
    duplicateCount: Int,
    onAddDuplicatesClick: () -> Unit,
    onSkipDuplicatesCount: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onAddDuplicatesClick) { Text(stringResource(R.string.add_anyway)) }
            TextButton(onClick = onSkipDuplicatesCount) { Text(stringResource(R.string.skip)) }
        },
        modifier = modifier,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    pluralStringResource(
                        id = R.plurals.x_selected_tracks_already_in_playlist,
                        count = duplicateCount,
                        duplicateCount,
                    )
                )
                Text(stringResource(R.string.what_do_you_want_to_do))
            }
        },
    )
}
