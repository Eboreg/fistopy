package us.huseli.fistopy.compose.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.huseli.fistopy.R
import us.huseli.fistopy.externalcontent.ImportBackend
import us.huseli.fistopy.stringResource

@Composable
fun BackendSelection(
    activeBackend: ImportBackend,
    onSelect: (ImportBackend) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ImportBackend.entries.forEach { backend ->
            InputChip(
                selected = activeBackend == backend,
                onClick = { onSelect(backend) },
                label = { Text(stringResource(backend.stringRes)) },
                leadingIcon = {
                    val drawableRes = when (backend) {
                        ImportBackend.LOCAL -> R.drawable.hard_drive_filled
                        ImportBackend.SPOTIFY -> R.drawable.spotify
                        ImportBackend.LAST_FM -> R.drawable.lastfm
                    }

                    Icon(painterResource(drawableRes), null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}
