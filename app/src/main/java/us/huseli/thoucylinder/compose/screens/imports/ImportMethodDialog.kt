package us.huseli.thoucylinder.compose.screens.imports

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.stringResource

@Composable
fun ImportMethodDialog(
    onDismissRequest: () -> Unit,
    onImportClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            SaveButton(
                text = stringResource(R.string.import_and_match_with_youtube),
                onClick = { onImportClick(true) },
            )
            SaveButton(
                text = stringResource(R.string.import_without_matching),
                onClick = { onImportClick(false) },
            )
        },
        dismissButton = { CancelButton(onClick = onDismissRequest) },
        title = title,
        text = text,
    )
}
