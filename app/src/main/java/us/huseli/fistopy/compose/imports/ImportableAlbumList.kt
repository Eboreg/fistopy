package us.huseli.fistopy.compose.imports

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.scrollbar.ScrollbarListState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarListState
import us.huseli.fistopy.compose.utils.ItemList
import us.huseli.fistopy.dataclasses.album.ImportableAlbumUiState
import us.huseli.fistopy.externalcontent.ImportBackend
import us.huseli.fistopy.stringResource

@Composable
fun ImportableAlbumList(
    uiStates: ImmutableList<ImportableAlbumUiState>,
    isLoadingCurrentPage: Boolean,
    isEmpty: Boolean,
    backend: ImportBackend,
    onGotoAlbumClick: (String) -> Unit,
    toggleSelected: (String) -> Unit,
    onLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollbarState: ScrollbarListState = rememberScrollbarListState(),
) {
    LaunchedEffect(uiStates.firstOrNull()) {
        if (uiStates.isNotEmpty()) scrollbarState.scrollToItem(0)
    }

    ItemList(
        things = { uiStates },
        key = { it.id },
        modifier = modifier,
        scrollbarState = scrollbarState,
        isLoading = isLoadingCurrentPage,
        loadingText = stringResource(R.string.loading),
        onEmpty = { if (isEmpty) Text(stringResource(R.string.no_albums_found)) },
        contentType = "IExternalAlbumWithTracks",
    ) { state ->
        ImportableAlbumCard(
            state = state,
            onClick = {
                if (state.isImported) onGotoAlbumClick(state.albumId)
                else toggleSelected(state.id)
            },
            onLongClick = { onLongClick(state.id) },
            backend = backend,
        )
    }
}
