package us.huseli.fistopy.compose.settings

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import us.huseli.fistopy.R
import us.huseli.fistopy.compose.utils.CancelButton
import us.huseli.fistopy.compose.utils.SaveButton
import us.huseli.fistopy.stringResource

@Composable
fun LocalMusicUriDialog(
    currentValue: Uri? = null,
    text: @Composable () -> Unit,
    title: @Composable () -> Unit = { Text(stringResource(R.string.local_music_directory)) },
    onCancelClick: () -> Unit = {},
    onSave: (Uri) -> Unit,
    cancelButtonText: String = stringResource(R.string.cancel),
    onDismissRequest: () -> Unit = onCancelClick,
) {
    val context = LocalContext.current
    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onSave(uri)
        }
    }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(onClick = onCancelClick, text = cancelButtonText) },
        confirmButton = {
            SaveButton(
                onClick = {
                    val input = currentValue ?: Environment.getExternalStorageDirectory()
                        .toUri()
                        .buildUpon()
                        .appendPath(Environment.DIRECTORY_MUSIC)
                        .build()
                    selectDirlauncher.launch(input)
                },
                text = stringResource(R.string.select_directory),
            )
        },
        title = title,
        text = text,
    )
}
