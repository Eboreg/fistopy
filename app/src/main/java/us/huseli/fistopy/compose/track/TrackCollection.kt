package us.huseli.fistopy.compose.track

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.fistopy.R
import us.huseli.fistopy.TrackDownloadTask
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.scrollbar.ScrollbarGridState
import us.huseli.fistopy.compose.scrollbar.ScrollbarListState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarGridState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarListState
import us.huseli.fistopy.dataclasses.track.TrackSelectionCallbacks
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.stringResource

@Composable
fun TrackCollection(
    states: () -> ImmutableList<TrackUiState>,
    displayType: DisplayType,
    getDownloadStateFlow: (String) -> StateFlow<TrackDownloadTask.UiState?>,
    onClick: (TrackUiState) -> Unit,
    onLongClick: (TrackUiState) -> Unit,
    selectedTrackCount: () -> Int,
    trackSelectionCallbacks: TrackSelectionCallbacks,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    scrollbarGridState: ScrollbarGridState = rememberScrollbarGridState(),
    scrollbarListState: ScrollbarListState = rememberScrollbarListState(),
    showAlbum: Boolean = false,
    showArtist: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    onEmpty: @Composable () -> Unit = { Text(stringResource(R.string.no_tracks_found)) },
) {
    when (displayType) {
        DisplayType.LIST -> TrackList(
            states = states,
            getDownloadStateFlow = getDownloadStateFlow,
            onClick = onClick,
            onLongClick = onLongClick,
            selectedTrackCount = selectedTrackCount,
            trackSelectionCallbacks = trackSelectionCallbacks,
            modifier = modifier,
            isLoading = isLoading,
            scrollbarState = scrollbarListState,
            showAlbum = showAlbum,
            showArtist = showArtist,
            onEmpty = onEmpty,
            trailingContent = trailingContent,
        )
        DisplayType.GRID -> TrackGrid(
            states = states,
            modifier = modifier,
            isLoading = isLoading,
            showArtist = showArtist,
            showAlbum = showAlbum,
            trackSelectionCallbacks = trackSelectionCallbacks,
            selectedTrackCount = selectedTrackCount,
            scrollbarState = scrollbarGridState,
            onClick = onClick,
            onLongClick = onLongClick,
            getDownloadStateFlow = getDownloadStateFlow,
            onEmpty = onEmpty,
            trailingContent = trailingContent,
        )
    }
}
