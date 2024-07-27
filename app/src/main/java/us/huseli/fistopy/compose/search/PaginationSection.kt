package us.huseli.fistopy.compose.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.utils.SmallOutlinedButton
import us.huseli.fistopy.stringResource

@Composable
fun PaginationSection(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallOutlinedButton(
            onClick = onPreviousClick,
            content = { Icon(Icons.Sharp.SkipPrevious, stringResource(R.string.previous)) },
            enabled = hasPrevious,
        )
        SmallOutlinedButton(
            onClick = onNextClick,
            content = { Icon(Icons.Sharp.SkipNext, stringResource(R.string.next)) },
            enabled = hasNext,
        )
    }
}
