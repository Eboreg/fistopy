package us.huseli.fistopy.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import us.huseli.fistopy.AbstractTrackUiStateListHandler
import us.huseli.fistopy.Constants.NAV_ARG_PLAYLIST
import us.huseli.fistopy.dataclasses.callbacks.AppDialogCallbacks
import us.huseli.fistopy.dataclasses.playlist.PlaylistUiState
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _playlistId = MutableStateFlow(savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!)
    private val _trackUiStates = MutableStateFlow<List<TrackUiState>>(emptyList())

    private val trackStateHandler =
        object : AbstractTrackUiStateListHandler<TrackUiState>(key = "playlist", repos = repos, managers = managers) {
            override val baseItems: Flow<List<TrackUiState>>
                get() = _trackUiStates
        }

    val isLoadingTracks = trackStateHandler.isLoadingItems
    val selectedTrackCount = trackStateHandler.selectedItemCount.stateWhileSubscribed(0)
    val trackUiStates = trackStateHandler.items.stateWhileSubscribed(persistentListOf())

    val playlistState: StateFlow<PlaylistUiState?> =
        combine(_playlistId, repos.playlist.playlistUiStates) { playlistId, states ->
            states.find { it.id == playlistId }
        }.stateWhileSubscribed()

    init {
        launchOnIOThread {
            _playlistId.collect {
                trackStateHandler.unselectAllItems()
            }
        }
        launchOnIOThread {
            repos.playlist.flowPlaylistTrackUiStates(_playlistId.value).collect { states ->
                _trackUiStates.value = states
            }
        }
    }

    fun deletePlaylist(onGotoPlaylistClick: () -> Unit) =
        managers.playlist.deletePlaylist(playlistId = _playlistId.value, onGotoPlaylistClick = onGotoPlaylistClick)

    fun getTrackDownloadUiStateFlow(trackId: String) =
        managers.library.getTrackDownloadUiStateFlow(trackId).stateWhileSubscribed()

    fun getTrackSelectionCallbacks(dialogCallbacks: AppDialogCallbacks) =
        trackStateHandler.getTrackSelectionCallbacks(dialogCallbacks)

    fun onMoveTrack(from: Int, to: Int) {
        _trackUiStates.value = _trackUiStates.value.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun onMoveTrackFinished(from: Int, to: Int) {
        launchOnIOThread { repos.playlist.movePlaylistTrack(_playlistId.value, from, to) }
    }

    fun onTrackClick(state: TrackUiState) = trackStateHandler.onTrackClick(state)

    fun onTrackLongClick(trackId: String) = trackStateHandler.onItemLongClick(trackId)

    fun playPlaylist() = managers.player.playPlaylist(_playlistId.value)

    fun removeSelectedPlaylistTracks() {
        launchOnIOThread {
            repos.playlist.removePlaylistTracks(_playlistId.value, trackStateHandler.selectedItemIds.first())
            trackStateHandler.unselectAllItems()
        }
    }

    fun renamePlaylist(newName: String) = managers.playlist.renamePlaylist(_playlistId.value, newName)
}
