package us.huseli.fistopy.compose.library

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.DisplayType
import us.huseli.fistopy.compose.scrollbar.ScrollbarGridState
import us.huseli.fistopy.compose.scrollbar.ScrollbarListState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarGridState
import us.huseli.fistopy.compose.scrollbar.rememberScrollbarListState
import us.huseli.fistopy.compose.track.TrackCollection
import us.huseli.fistopy.compose.utils.EmptyLibraryHelp
import us.huseli.fistopy.compose.utils.ListActions
import us.huseli.fistopy.dataclasses.callbacks.LocalAppDialogCallbacks
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.TrackSortParameter
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenTrackTab(
    uiStates: () -> ImmutableList<TrackUiState>,
    modifier: Modifier = Modifier,
    listState: ScrollbarListState = rememberScrollbarListState(),
    gridState: ScrollbarGridState = rememberScrollbarGridState(),
    displayType: DisplayType = DisplayType.LIST,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val dialogCallbacks = LocalAppDialogCallbacks.current
    val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
    val isTracksEmpty by viewModel.isTracksEmpty.collectAsStateWithLifecycle()
    val selectedTrackCount = viewModel.selectedTrackCount.collectAsStateWithLifecycle().asIntState()
    val trackSelectionCallbacks = remember { viewModel.getTrackSelectionCallbacks(dialogCallbacks) }

    TrackCollection(
        states = uiStates,
        displayType = displayType,
        getDownloadStateFlow = { viewModel.getTrackDownloadUiStateFlow(it) },
        onClick = remember { { viewModel.onTrackClick(it) } },
        onLongClick = remember { { viewModel.onTrackLongClick(it.id) } },
        selectedTrackCount = { selectedTrackCount.intValue },
        trackSelectionCallbacks = trackSelectionCallbacks,
        modifier = modifier,
        isLoading = isLoadingTracks,
        scrollbarGridState = gridState,
        scrollbarListState = listState,
        showAlbum = true,
        showArtist = true,
        onEmpty = {
            if (isTracksEmpty) {
                Text(stringResource(R.string.no_tracks_found))
                EmptyLibraryHelp()
            }
        },
    )
}

@Composable
fun TrackListActions(viewModel: LibraryViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()
    val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
    val selectedTagPojos by viewModel.selectedTrackTagPojos.collectAsStateWithLifecycle()
    val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
    val tagPojos by viewModel.trackTagPojos.collectAsStateWithLifecycle()

    ListActions(
        searchTerm = { searchTerm },
        sortParameter = sortParameter,
        sortOrder = sortOrder,
        sortParameters = remember { TrackSortParameter.withLabels(context) },
        sortDialogTitle = stringResource(R.string.track_order),
        onSort = remember { { param, order -> viewModel.setTrackSorting(param, order) } },
        onSearch = remember { { viewModel.setTrackSearchTerm(it) } },
        filterButtonSelected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
        tagPojos = { tagPojos },
        selectedTagPojos = { selectedTagPojos },
        availabilityFilter = availabilityFilter,
        onTagsChange = remember { { viewModel.setSelectedTrackTagPojos(it) } },
        onAvailabilityFilterChange = remember { { viewModel.setAvailabilityFilter(it) } },
    )
}
