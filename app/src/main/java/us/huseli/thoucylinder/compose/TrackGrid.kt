package us.huseli.thoucylinder.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import us.huseli.retaintheme.sensibleFormat
import us.huseli.thoucylinder.Selection
import us.huseli.thoucylinder.dataclasses.Track
import us.huseli.thoucylinder.repositories.PlayerRepository
import us.huseli.thoucylinder.themeColors
import us.huseli.thoucylinder.viewmodels.BaseViewModel
import java.util.UUID

@Composable
fun TrackGrid(
    tracks: LazyPagingItems<Track>,
    viewModel: BaseViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    onAddToPlaylistClick: (Selection) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
) {
    TrackGrid(
        viewModel = viewModel,
        gridState = gridState,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        trackIterator = { action ->
            items(count = tracks.itemCount, key = tracks.itemKey { it.trackId }) { index ->
                tracks[index]?.also { track -> action(this, track) }
            }
        },
        onLaunch = onLaunch,
        showArtist = showArtist,
        onAddToPlaylistClick = onAddToPlaylistClick,
    )
}


@Composable
fun TrackGrid(
    tracks: List<Track>,
    viewModel: BaseViewModel,
    gridState: LazyGridState = rememberLazyGridState(),
    showArtist: Boolean = true,
    onAddToPlaylistClick: (Selection) -> Unit,
    onGotoArtistClick: ((String) -> Unit)? = null,
    onGotoAlbumClick: ((UUID) -> Unit)? = null,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)? = null,
) {
    TrackGrid(
        viewModel = viewModel,
        gridState = gridState,
        onGotoArtistClick = onGotoArtistClick,
        onGotoAlbumClick = onGotoAlbumClick,
        trackIterator = { action ->
            items(tracks) { track -> action(this, track) }
        },
        onLaunch = onLaunch,
        showArtist = showArtist,
        onAddToPlaylistClick = onAddToPlaylistClick,
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackGrid(
    viewModel: BaseViewModel,
    gridState: LazyGridState,
    trackIterator: LazyGridScope.(@Composable LazyGridItemScope.(Track) -> Unit) -> Unit,
    showArtist: Boolean,
    onAddToPlaylistClick: (Selection) -> Unit,
    onGotoArtistClick: ((String) -> Unit)?,
    onGotoAlbumClick: ((UUID) -> Unit)?,
    onLaunch: (suspend CoroutineScope.(Track) -> Unit)?,
) {
    val downloadProgressMap by viewModel.trackDownloadProgressMap.collectAsStateWithLifecycle()
    val playerCurrentTrack by viewModel.playerCurrentTrack.collectAsStateWithLifecycle()
    val playerPlaybackState by viewModel.playerPlaybackState.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        SelectedTracksButtons(
            selection = selection,
            onAddToPlaylistClick = { onAddToPlaylistClick(selection) },
            onUnselectAllClick = { viewModel.unselectAllTracks() },
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            trackIterator { track ->
                val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
                var isContextMenuShown by rememberSaveable { mutableStateOf(false) }
                val isPlaying = playerCurrentTrack?.trackId == track.trackId &&
                    playerPlaybackState == PlayerRepository.PlaybackState.PLAYING
                val isSelected = selection.isTrackSelected(track)

                if (onLaunch != null) LaunchedEffect(Unit) {
                    onLaunch(track)
                }

                LaunchedEffect(Unit) {
                    track.image?.let { imageBitmap.value = viewModel.getImageBitmap(it) }
                }

                OutlinedCard(
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.combinedClickable(
                        onClick = { if (selection.tracks.isNotEmpty()) viewModel.toggleTrackSelected(track) },
                        onLongClick = { viewModel.toggleTrackSelected(track) },
                    ),
                    border = CardDefaults.outlinedCardBorder()
                        .let { if (isSelected) it.copy(width = it.width + 2.dp) else it },
                ) {
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        AlbumArt(
                            image = imageBitmap.value,
                            modifier = Modifier.fillMaxWidth(),
                            topContent = {
                                Row {
                                    Column(modifier = Modifier.weight(1f)) {
                                        track.metadata?.duration?.let { duration ->
                                            Surface(
                                                shape = CutCornerShape(bottomEndPercent = 100),
                                                color = MaterialTheme.colorScheme.error,
                                                contentColor = contentColorFor(MaterialTheme.colorScheme.error),
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(50.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = duration.sensibleFormat(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.rotate(-45f).offset(0.dp, (-10).dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    FilledTonalIconButton(
                                        onClick = { isContextMenuShown = !isContextMenuShown },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.error),
                                        ),
                                        modifier = Modifier.size(50.dp),
                                        shape = CutCornerShape(bottomStartPercent = 100),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Sharp.MoreVert,
                                            contentDescription = null,
                                            modifier = Modifier.rotate(-45f).offset(10.dp, 0.dp),
                                        )
                                        TrackContextMenu(
                                            track = track,
                                            onDownloadClick = { viewModel.downloadTrack(track) },
                                            onDismissRequest = { isContextMenuShown = false },
                                            isShown = isContextMenuShown,
                                            onGotoAlbumClick = onGotoAlbumClick,
                                            onGotoArtistClick = onGotoArtistClick,
                                            offset = DpOffset(0.dp, (-20).dp),
                                            onAddToPlaylistClick = {
                                                onAddToPlaylistClick(Selection(tracks = listOf(track)))
                                            },
                                        )
                                    }
                                }
                            },
                            bottomContent = {
                                FilledTonalIconButton(
                                    onClick = { viewModel.playOrPause(track) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = contentColorFor(MaterialTheme.colorScheme.error),
                                    ),
                                    modifier = Modifier.size(50.dp).align(Alignment.End),
                                    shape = CutCornerShape(topStartPercent = 100),
                                    content = {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.offset(10.dp, 10.dp),
                                        )
                                    }
                                )
                            }
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Sharp.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                tint = themeColors().Green.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.padding(5.dp).weight(1f)) {
                            ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                                val artist = track.artist?.takeIf { it.isNotBlank() && showArtist }
                                val titleLines = if (artist != null) 1 else 2

                                Text(
                                    text = track.title,
                                    maxLines = titleLines,
                                    minLines = titleLines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (artist != null) {
                                    Text(text = artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    downloadProgressMap[track.trackId]?.let { progress ->
                        val statusText = stringResource(progress.status.stringId)

                        Column(modifier = Modifier.padding(bottom = 5.dp)) {
                            Text(text = "$statusText …")
                            LinearProgressIndicator(
                                progress = progress.progress.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}