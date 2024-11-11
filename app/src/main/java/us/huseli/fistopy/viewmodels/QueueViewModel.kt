package us.huseli.fistopy.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractTrackUiStateListHandler
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.radio.RadioUiState
import us.huseli.fistopy.dataclasses.track.AbstractTrackUiState
import us.huseli.fistopy.dataclasses.track.QueueTrackCombo
import us.huseli.fistopy.dataclasses.track.TrackSelectionCallbacks
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.dataclasses.track.toUiStates
import us.huseli.fistopy.enums.RadioStatus
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.retaintheme.extensions.launchOnMainThread
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _queue = MutableStateFlow<List<QueueTrackCombo>>(emptyList())
    private val isRadioLoading =
        managers.radio.radioStatus.map { it == RadioStatus.LOADING || it == RadioStatus.LOADING_MORE }

    private val trackStateHandler =
        object : AbstractTrackUiStateListHandler<TrackUiState>(key = "queue", repos = repos, managers = managers) {
            override val baseItems: Flow<List<TrackUiState>> = _queue.map { it.toUiStates() }

            override fun enqueueSelectedTracks() {
                launchOnMainThread { managers.player.moveTracksNext(queueTrackIds = getSortedSelectedItems().map { it.id }) }
            }

            /** It makes little sense to define onPlayClick and onEnqueueClick here. */
            override fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks): TrackSelectionCallbacks =
                super.getTrackSelectionCallbacks(dialogCallbacks).copy(
                    onEnqueueClick = null,
                    onPlayClick = null,
                )

            override fun playSelectedTracks() {
                launchOnMainThread { repos.player.moveNextAndPlay(queueTrackIds = getSortedSelectedItems().map { it.id }) }
            }

            override fun playTrack(state: AbstractTrackUiState) {
                _queue.value
                    .indexOfFirst { it.queueTrackId == state.id }
                    .takeIf { it >= 0 }
                    ?.also { repos.player.skipTo(it) }
            }
        }

    val currentComboId: StateFlow<String?> =
        repos.player.currentCombo.map { it?.queueTrackId }.distinctUntilChanged().stateWhileSubscribed()
    val currentComboIndex: StateFlow<Int?> = combine(repos.player.queue, repos.player.currentCombo) { queue, combo ->
        queue.indexOf(combo).takeIf { it > -1 }
    }.stateWhileSubscribed()
    val isLoading = combine(trackStateHandler.isLoadingItems, isRadioLoading) { isLoading, isRadioLoading ->
        isRadioLoading || isLoading
    }.stateWhileSubscribed(true)
    val radioUiState: StateFlow<RadioUiState?> =
        managers.radio.radioUiState.distinctUntilChanged().stateWhileSubscribed()
    val selectedTrackCount = trackStateHandler.selectedItemCount
    val trackUiStates = trackStateHandler.items

    init {
        launchOnIOThread {
            repos.player.queue.collect { queue ->
                _queue.value = queue
            }
        }
    }

    fun clearQueue() = repos.player.clearQueue()

    fun deactivateRadio() = managers.radio.deactivateRadio()

    fun enqueueTrack(queueTrackId: String) {
        managers.player.moveTracksNext(listOf(queueTrackId))
    }

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks): TrackSelectionCallbacks =
        trackStateHandler.getTrackSelectionCallbacks(dialogCallbacks)

    fun onMoveTrack(from: Int, to: Int) {
        /**
         * Only does visual move while reordering, does not store anything. Call onMoveTrackFinished() when reorder
         * operation is finished.
         */
        _queue.value = _queue.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) = repos.player.onMoveTrackFinished(from, to)

    fun onTrackClick(state: TrackUiState) = trackStateHandler.onTrackClick(state)

    fun onTrackLongClick(trackId: String) = trackStateHandler.onItemLongClick(trackId)

    fun playTrack(state: AbstractTrackUiState) = trackStateHandler.playTrack(state)

    fun removeFromQueue(queueTrackId: String) {
        repos.player.removeFromQueue(listOf(queueTrackId))
        trackStateHandler.setItemsIsSelected(listOf(queueTrackId), false)
    }

    fun removeSelectedTracksFromQueue() {
        launchOnMainThread {
            repos.player.removeFromQueue(queueTrackIds = trackStateHandler.selectedItemIds.first())
            trackStateHandler.unselectAllItems()
        }
    }
}
