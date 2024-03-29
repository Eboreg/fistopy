package us.huseli.thoucylinder.compose.screens.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import kotlinx.collections.immutable.toImmutableList
import us.huseli.thoucylinder.enums.AvailabilityFilter
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.enums.TrackSortParameter
import us.huseli.thoucylinder.compose.DisplayType
import us.huseli.thoucylinder.compose.ListSettingsRow
import us.huseli.thoucylinder.compose.ListType
import us.huseli.thoucylinder.compose.track.TrackGrid
import us.huseli.thoucylinder.compose.track.TrackList
import us.huseli.thoucylinder.compose.utils.CollapsibleToolbar
import us.huseli.thoucylinder.compose.utils.ListActions
import us.huseli.thoucylinder.dataclasses.callbacks.AppCallbacks
import us.huseli.thoucylinder.dataclasses.callbacks.TrackCallbacks
import us.huseli.thoucylinder.dataclasses.views.TrackCombo
import us.huseli.thoucylinder.stringResource
import us.huseli.thoucylinder.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenTrackTab(
    trackCombos: LazyPagingItems<TrackCombo>,
    isImporting: Boolean,
    viewModel: LibraryViewModel,
    appCallbacks: AppCallbacks,
    displayType: DisplayType,
    gridState: LazyGridState,
    listState: LazyListState,
    showToolbars: Boolean,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isLoadingTracks by viewModel.isLoadingTracks.collectAsStateWithLifecycle()
    val latestSelectedTrackId by viewModel.latestSelectedTrackId.collectAsStateWithLifecycle(null)
    val searchTerm by viewModel.trackSearchTerm.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.trackSortOrder.collectAsStateWithLifecycle()
    val sortParameter by viewModel.trackSortParameter.collectAsStateWithLifecycle()
    val trackDownloadTasks by viewModel.trackDownloadTasks.collectAsStateWithLifecycle(emptyList())
    val tagPojos by viewModel.trackTagPojos.collectAsStateWithLifecycle(emptyList())
    val selectedTagPojos by viewModel.selectedTrackTagPojos.collectAsStateWithLifecycle()
    val availabilityFilter by viewModel.availabilityFilter.collectAsStateWithLifecycle()

    var latestSelectedIndex by rememberSaveable(selectedTrackIds) { mutableStateOf<Int?>(null) }

    val trackCallbacks = remember {
        { index: Int, combo: TrackCombo ->
            TrackCallbacks(
                combo = combo,
                appCallbacks = appCallbacks,
                context = context,
                onTrackClick = {
                    if (selectedTrackIds.isNotEmpty()) viewModel.toggleTrackSelected(combo.track.trackId)
                    else if (combo.track.isPlayable) viewModel.playTrackCombo(combo)
                },
                onEnqueueClick = if (combo.track.isPlayable) {
                    { viewModel.enqueueTrackCombo(combo, context) }
                } else null,
                onLongClick = {
                    if (selectedTrackIds.isEmpty()) viewModel.toggleTrackSelected(combo.track.trackId)
                    else viewModel.selectTracksBetweenIndices(
                        fromIndex = latestSelectedIndex,
                        toIndex = index,
                        getTrackIdAtIndex = { trackCombos[it]?.track?.trackId },
                    )
                },
                onEach = {
                    if (combo.track.trackId == latestSelectedTrackId)
                        latestSelectedIndex = index
                },
            )
        }
    }

    val trackSelectionCallbacks = remember { viewModel.getTrackSelectionCallbacks(appCallbacks, context) }

    val progressIndicatorText =
        if (isImporting) stringResource(R.string.importing_local_tracks)
        else if (isLoadingTracks) stringResource(R.string.loading_tracks)
        else null

    val onEmpty: @Composable () -> Unit = {
        if (!isImporting && !isLoadingTracks) {
            Text(
                stringResource(R.string.no_tracks_found),
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    CollapsibleToolbar(show = showToolbars) {
        ListSettingsRow(
            displayType = displayType,
            listType = ListType.TRACKS,
            onDisplayTypeChange = { viewModel.setDisplayType(it) },
            onListTypeChange = { viewModel.setListType(it) },
            availableDisplayTypes = listOf(DisplayType.LIST, DisplayType.GRID),
        )
        ListActions(
            initialSearchTerm = searchTerm,
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            sortParameters = TrackSortParameter.withLabels(context),
            sortDialogTitle = stringResource(R.string.track_order),
            onSort = { param, order -> viewModel.setTrackSorting(param, order) },
            onSearch = { viewModel.setTrackSearchTerm(it) },
            modifier = Modifier.animateContentSize().heightIn(max = if (showToolbars) Dp.Infinity else 0.dp),
            filterButtonSelected = selectedTagPojos.isNotEmpty() || availabilityFilter != AvailabilityFilter.ALL,
            tagPojos = tagPojos.toImmutableList(),
            selectedTagPojos = selectedTagPojos,
            availabilityFilter = availabilityFilter,
            onTagsChange = { viewModel.setSelectedTrackTagPojos(it) },
            onAvailabilityFilterChange = { viewModel.setAvailabilityFilter(it) },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        when (displayType) {
            DisplayType.LIST -> TrackList(
                trackCombos = trackCombos,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                viewModel = viewModel,
                listState = listState,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                trackDownloadTasks = trackDownloadTasks.toImmutableList(),
                onEmpty = onEmpty,
                progressIndicatorText = progressIndicatorText,
                modifier = listModifier,
            )
            DisplayType.GRID -> TrackGrid(
                trackCombos = trackCombos,
                viewModel = viewModel,
                gridState = gridState,
                trackCallbacks = trackCallbacks,
                trackSelectionCallbacks = trackSelectionCallbacks,
                selectedTrackIds = selectedTrackIds.toImmutableList(),
                trackDownloadTasks = trackDownloadTasks.toImmutableList(),
                onEmpty = onEmpty,
                progressIndicatorText = progressIndicatorText,
                modifier = listModifier,
            )
        }
    }
}
