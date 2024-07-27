package us.huseli.fistopy.compose.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.dataclasses.artist.ArtistUiState

@Composable
fun ArtistCollection(
    states: () -> ImmutableList<ArtistUiState>,
    displayType: DisplayType,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onEmpty: @Composable () -> Unit = {},
) {
    when (displayType) {
        DisplayType.LIST -> ArtistList(
            states = states,
            modifier = modifier,
            isLoading = isLoading,
            onEmpty = onEmpty,
        )
        DisplayType.GRID -> ArtistGrid(
            uiStates = states,
            modifier = modifier,
            isLoading = isLoading,
            onEmpty = onEmpty,
        )
    }
}
