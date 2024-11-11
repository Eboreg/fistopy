package us.huseli.fistopy.compose.modalcover

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import us.huseli.fistopy.dataclasses.ModalCoverBooleans
import us.huseli.fistopy.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.fistopy.dataclasses.track.ModalCoverTrackUiState
import kotlin.math.roundToInt

enum class HorizontalDragValue { Previous, Current, Next }


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumArtAndTitlesSwipeable(
    state: ModalCoverState,
    trackUiState: ModalCoverTrackUiState,
    nextTrackUiState: ModalCoverTrackUiState?,
    previousTrackUiState: ModalCoverTrackUiState?,
    playbackCallbacks: PlaybackCallbacks,
    booleans: ModalCoverBooleans,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    var nextTrackUiStateMutable by remember { mutableStateOf(nextTrackUiState) }
    var previousTrackUiStateMutable by remember { mutableStateOf(previousTrackUiState) }
    var currentDragValue by remember { mutableStateOf(HorizontalDragValue.Current) }

    val anchors = remember(previousTrackUiState == null, nextTrackUiState == null) {
        DraggableAnchors {
            if (previousTrackUiState != null) HorizontalDragValue.Previous at screenWidthPx
            else HorizontalDragValue.Previous at 0f
            if (nextTrackUiState != null) HorizontalDragValue.Next at -screenWidthPx
            else HorizontalDragValue.Next at 0f
            HorizontalDragValue.Current at 0f
        }
    }

    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = HorizontalDragValue.Current,
            anchors = anchors,
            positionalThreshold = { it * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = SpringSpec(),
            decayAnimationSpec = decayAnimationSpec,
            confirmValueChange = { newValue ->
                currentDragValue = newValue
                true
            },
        )
    }

    LaunchedEffect(currentDragValue) {
        when (currentDragValue) {
            HorizontalDragValue.Previous -> playbackCallbacks.skipToPrevious()
            HorizontalDragValue.Current -> {}
            HorizontalDragValue.Next -> playbackCallbacks.skipToNext()
        }
    }

    LaunchedEffect(previousTrackUiState == null, nextTrackUiState == null) {
        draggableState.updateAnchors(anchors, HorizontalDragValue.Current)
    }

    LaunchedEffect(trackUiState.id) {
        nextTrackUiStateMutable = nextTrackUiState
        previousTrackUiStateMutable = previousTrackUiState
        draggableState.snapTo(HorizontalDragValue.Current)
    }

    Box(
        modifier = modifier
            .anchoredDraggable(state = draggableState, orientation = Orientation.Horizontal)
            .offset(x = -screenWidthDp)
            .then(
                if (state.isExpanded)
                    Modifier.offset { IntOffset(x = draggableState.requireOffset().roundToInt(), y = 0) }
                else Modifier
            )
            .width(screenWidthDp * 3)
    ) {
        // Previous track thumbnail & titles:
        previousTrackUiStateMutable?.also { uiState ->
            AlbumArtAndTitlesColumn(
                state = state,
                albumArtModel = uiState,
                title = uiState.title,
                artist = uiState.artistString,
                offsetX = 0,
                playbackCallbacks = playbackCallbacks,
                booleans = booleans,
            )
        } ?: run { Box(modifier = Modifier.width(screenWidthDp)) }

        AlbumArtAndTitlesColumn(
            state = state,
            albumArtModel = trackUiState,
            title = trackUiState.title,
            artist = trackUiState.artistString,
            offsetX = screenWidthPx.toInt(),
            playbackCallbacks = playbackCallbacks,
            booleans = booleans,
        )

        // Next track thumbnail & titles:
        nextTrackUiStateMutable?.also { uiState ->
            AlbumArtAndTitlesColumn(
                state = state,
                albumArtModel = uiState,
                title = uiState.title,
                artist = uiState.artistString,
                offsetX = screenWidthPx.toInt() * 2,
                playbackCallbacks = playbackCallbacks,
                booleans = booleans,
            )
        } ?: run { Box(modifier = Modifier.width(screenWidthDp).offset(x = screenWidthDp * 2)) }
    }
}
