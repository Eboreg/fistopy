package us.huseli.fistopy.compose.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import us.huseli.fistopy.AlbumDownloadTask
import us.huseli.fistopy.compose.scrollbar.ScrollbarListState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarListState
import us.huseli.fistopy.compose.utils.ItemList
import us.huseli.fistopy.dataclasses.album.AlbumSelectionCallbacks
import us.huseli.fistopy.dataclasses.album.IAlbumUiState

@Composable
fun AlbumList(
    states: () -> ImmutableList<IAlbumUiState>,
    downloadStateFlow: (String) -> StateFlow<AlbumDownloadTask.UiState?>,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    selectedAlbumCount: () -> Int,
    selectionCallbacks: AlbumSelectionCallbacks,
    modifier: Modifier = Modifier,
    showArtist: Boolean = true,
    isLoading: Boolean = false,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
    onEmpty: @Composable () -> Unit = {},
) {
    SelectedAlbumsButtons(albumCount = selectedAlbumCount, callbacks = selectionCallbacks)

    ItemList(
        things = states,
        key = { it.albumId },
        scrollbarState = scrollbarState,
        isLoading = isLoading,
        onEmpty = onEmpty,
        modifier = modifier,
        contentType = "AlbumUiState",
    ) { state ->
        AlbumListCard(
            state = state,
            downloadStateFlow = { downloadStateFlow(state.albumId) },
            onClick = { onClick(state.albumId) },
            onLongClick = { onLongClick(state.albumId) },
            showArtist = showArtist,
        )
    }
}
