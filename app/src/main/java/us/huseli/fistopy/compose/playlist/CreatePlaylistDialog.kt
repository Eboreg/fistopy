package us.huseli.fistopy.compose.playlist

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import us.huseli.retaintheme.extensions.nullIfBlank
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.utils.CancelButton
import us.huseli.fistopy.compose.utils.OutlinedTextFieldLabel
import us.huseli.fistopy.compose.utils.SaveButton
import us.huseli.fistopy.stringResource

@Composable
fun CreatePlaylistDialog(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onCancel,
        dismissButton = { CancelButton(onClick = onCancel) },
        confirmButton = {
            SaveButton(enabled = name.isNotBlank()) {
                name.nullIfBlank()?.also(onSave)
            }
        },
        title = { Text(stringResource(R.string.add_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { OutlinedTextFieldLabel(text = stringResource(R.string.name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            )
        },
    )
}
