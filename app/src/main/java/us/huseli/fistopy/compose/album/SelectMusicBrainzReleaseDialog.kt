package us.huseli.fistopy.compose.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.FistopyTheme
import us.huseli.fistopy.compose.utils.CancelButton
import us.huseli.fistopy.compose.utils.ItemList
import us.huseli.fistopy.compose.utils.ItemListCardWithThumbnail
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRelease
import us.huseli.fistopy.pluralStringResource
import us.huseli.fistopy.stringResource

@Composable
fun SelectMusicBrainzReleaseDialog(
    releases: ImmutableList<MusicBrainzRelease>,
    onSelect: (MusicBrainzRelease) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        modifier = Modifier.padding(20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(onClick = onDismissRequest) },
        confirmButton = { },
        title = { Text(stringResource(R.string.select_another_release)) },
        text = {
            if (releases.size < 2) {
                Text(stringResource(R.string.no_alternative_releases_found))
            } else {
                ItemList(things = { releases }, key = { it.id }) { release ->
                    val row2 = listOfNotNull(
                        release.date?.takeIf { it.isNotEmpty() },
                        release.getCountryName(context),
                    ).joinToString(" • ")
                    val row3 = listOfNotNull(
                        release.mediaFormat,
                        pluralStringResource(R.plurals.x_tracks, release.trackCount, release.trackCount),
                    ).joinToString(" • ")

                    ItemListCardWithThumbnail(
                        thumbnailModel = release,
                        thumbnailPlaceholder = Icons.Sharp.Album,
                        containerColor = AlertDialogDefaults.containerColor,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onSelect(release) },
                    ) {
                        Column {
                            Text(text = release.title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                            Text(
                                text = row2,
                                style = FistopyTheme.bodyStyles.primaryExtraSmall,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                            Text(
                                text = row3,
                                style = FistopyTheme.bodyStyles.primaryExtraSmall,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
    )
}
