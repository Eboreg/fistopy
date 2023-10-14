package us.huseli.thoucylinder.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retaintheme.compose.ListWithNumericBar
import us.huseli.retaintheme.compose.SmallOutlinedButton
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.compose.SelectedTracksButtons
import us.huseli.thoucylinder.compose.TrackListRow
import us.huseli.thoucylinder.dataclasses.TrackQueue
import us.huseli.thoucylinder.viewmodels.QueueViewModel
import java.util.UUID

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    onAddToPlaylistClick: (Selection) -> Unit,
    onAlbumClick: (UUID) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    QueueTrackList(
        queue = queue,
        viewModel = viewModel,
        modifier = modifier,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
    )
}


@Composable
fun QueueTrackList(
    queue: TrackQueue,
    viewModel: QueueViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onAddToPlaylistClick: (Selection) -> Unit,
    onAlbumClick: (UUID) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val selectedTracks by viewModel.selectedQueueTracks.collectAsStateWithLifecycle()
    val playerCurrentPojo by viewModel.playerCurrentPojo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            trackCount = selectedTracks.size,
            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(queueTracks = selectedTracks)) },
            onUnselectAllClick = { viewModel.unselectAllQueueTracks() },
            extraButtons = {
                SmallOutlinedButton(
                    onClick = { viewModel.removeFromQueue(selectedTracks) },
                    text = stringResource(R.string.remove),
                )
            },
        )

        ListWithNumericBar(listState = listState, listSize = queue.items.size) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                itemsIndexed(queue.items) { pojoIdx, pojo ->
                    val thumbnail = remember { mutableStateOf<ImageBitmap?>(null) }
                    var metadata by rememberSaveable { mutableStateOf(pojo.track.metadata) }
                    val isSelected = selectedTracks.contains(pojo)
                    val containerColor =
                        if (!isSelected && playerCurrentPojo == pojo)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else null

                    LaunchedEffect(pojo.track.trackId) {
                        pojo.track.image?.let { thumbnail.value = viewModel.getImageBitmap(it) }
                        if (metadata == null) metadata = viewModel.getTrackMetadata(pojo.track)
                    }

                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        TrackListRow(
                            track = pojo.track,
                            album = pojo.album,
                            metadata = metadata,
                            showArtist = true,
                            onDownloadClick = { viewModel.downloadTrack(pojo.track) },
                            onPlayClick = { viewModel.skipTo(pojoIdx) },
                            onArtistClick = onArtistClick,
                            onAlbumClick = onAlbumClick,
                            downloadProgress = downloadProgressMap[pojo.track.trackId],
                            onAddToPlaylistClick = { onAddToPlaylistClick(Selection(pojo.track)) },
                            onToggleSelected = { viewModel.toggleSelected(pojo) },
                            isSelected = isSelected,
                            selectOnShortClick = selectedTracks.isNotEmpty(),
                            thumbnail = thumbnail.value,
                            containerColor = containerColor,
                            onEnqueueNextClick = { viewModel.enqueueTrackNext(pojo.track, context) },
                        )
                    }
                }
            }
        }
    }
}
