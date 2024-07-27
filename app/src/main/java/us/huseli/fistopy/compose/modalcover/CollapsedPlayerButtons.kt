package us.huseli.fistopy.compose.modalcover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.LocalThemeSizes
import us.huseli.fistopy.compose.utils.LargerIconButton
import us.huseli.fistopy.dataclasses.ModalCoverBooleans
import us.huseli.fistopy.dataclasses.callbacks.PlaybackCallbacks
import us.huseli.fistopy.stringResource

@Composable
fun CollapsedPlayerButtons(
    booleans: ModalCoverBooleans,
    playbackCallbacks: PlaybackCallbacks,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val sizes = LocalThemeSizes.current
    val colors =
        IconButtonDefaults.iconButtonColors(contentColor = LocalContentColor.current.copy(alpha = alpha))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier,
    ) {
        IconButton(
            onClick = playbackCallbacks.playOrPauseCurrent,
            content = {
                if (booleans.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        if (booleans.isPlaying) Icons.Sharp.Pause else Icons.Sharp.PlayArrow,
                        if (booleans.isPlaying) stringResource(R.string.pause)
                        else stringResource(R.string.play),
                        modifier = Modifier.size(sizes.largerIconButtonIcon),
                    )
                }
            },
            enabled = booleans.canPlay,
            modifier = Modifier.size(sizes.largerIconButton),
            colors = colors,
        )
        LargerIconButton(
            icon = Icons.Sharp.SkipNext,
            onClick = playbackCallbacks.skipToNext,
            description = stringResource(R.string.next),
            colors = colors,
            enabled = booleans.canGotoNext,
        )
    }
}
