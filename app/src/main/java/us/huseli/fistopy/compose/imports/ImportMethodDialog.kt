package us.huseli.fistopy.compose.imports

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.utils.CancelButton
import us.huseli.fistopy.compose.utils.SaveButton
import us.huseli.fistopy.stringResource

@Composable
fun ImportMethodDialog(
    onDismissRequest: () -> Unit,
    onImportClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            OutlinedButton(
                content = { Text(stringResource(R.string.import_without_matching)) },
                shape = MaterialTheme.shapes.small,
                onClick = { onImportClick(false) },
            )
            SaveButton(
                text = stringResource(R.string.import_and_match_with_youtube),
                onClick = { onImportClick(true) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismissRequest) },
        title = title,
        text = text,
    )
}
