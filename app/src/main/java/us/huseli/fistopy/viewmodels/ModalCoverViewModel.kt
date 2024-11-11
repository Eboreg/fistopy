package us.huseli.fistopy.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.dataclasses.ModalCoverBooleans
import us.huseli.fistopy.dataclasses.track.ModalCoverTrackUiState
import us.huseli.fistopy.getAverageColor
import us.huseli.fistopy.getBitmap
import us.huseli.fistopy.repositories.Repositories
import us.huseli.fistopy.waveList
import us.huseli.retaintheme.extensions.square
import javax.inject.Inject
import kotlin.time.DurationUnit

@HiltViewModel
class ModalCoverViewModel @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext context: Context,
) : AbstractBaseViewModel() {
    val albumArtAverageColor: StateFlow<Color?> = repos.player.currentCombo.map { combo ->
        combo?.let { context.getBitmap(combo, 100)?.square()?.asImageBitmap() }
            ?.getAverageColor()
            ?.copy(alpha = 0.3f)
    }.stateWhileSubscribed()
    val isLoading: StateFlow<Boolean> = repos.player.isLoading

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentAmplitudes: StateFlow<ImmutableList<Int>> = repos.player.currentCombo.flatMapLatest { combo ->
        combo?.track?.let { repos.track.flowAmplitudes(it.trackId) } ?: emptyFlow()
    }.stateWhileSubscribed(waveList(100, 0, 12, 3).toImmutableList())

    val booleans = combine(
        repos.player.canGotoNext,
        repos.player.canPlay,
        repos.player.isLoading,
        repos.player.isPlaying,
        repos.player.isRepeatEnabled,
        repos.player.isShuffleEnabled,
    ) { booleans ->
        ModalCoverBooleans(
            canGotoNext = booleans[0],
            canPlay = booleans[1],
            isLoading = booleans[2],
            isPlaying = booleans[3],
            isRepeatEnabled = booleans[4],
            isShuffleEnabled = booleans[5],
        )
    }.distinctUntilChanged().stateWhileSubscribed(ModalCoverBooleans())

    val currentProgress: StateFlow<Float> =
        combine(repos.player.currentPositionMs, repos.player.currentCombo) { position, combo ->
            combo?.track?.duration?.toLong(DurationUnit.MILLISECONDS)
                ?.takeIf { it > 0 }
                ?.let { position / it.toFloat() }
                ?: 0f
        }.distinctUntilChanged().stateWhileSubscribed(0f)

    val nextTrackUiState: StateFlow<ModalCoverTrackUiState?> = repos.player.nextCombo.map { combo ->
        combo?.let { ModalCoverTrackUiState.fromTrackCombo(it) }
    }.distinctUntilChanged().stateWhileSubscribed()

    val previousTrackUiState: StateFlow<ModalCoverTrackUiState?> = repos.player.previousCombo.map { combo ->
        combo?.let { ModalCoverTrackUiState.fromTrackCombo(it) }
    }.distinctUntilChanged().stateWhileSubscribed()

    val trackUiState: StateFlow<ModalCoverTrackUiState?> = repos.player.currentCombo.map { combo ->
        combo?.let { ModalCoverTrackUiState.fromTrackCombo(it) }
    }.distinctUntilChanged().stateWhileSubscribed()

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun seekToProgress(progress: Float) = repos.player.seekToProgress(progress)

    fun skipToNext() = repos.player.skipToNext()

    fun skipToPrevious() = repos.player.skipToPrevious()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleShuffle() = repos.player.toggleShuffle()
}
