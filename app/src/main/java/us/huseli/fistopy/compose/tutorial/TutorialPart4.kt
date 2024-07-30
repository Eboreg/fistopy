package us.huseli.fistopy.compose.tutorial

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import us.huseli.fistopy.R
import us.huseli.fistopy.stringResource

@Composable
fun TutorialPart4(onFinish: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onFinish() }

        Text(stringResource(R.string.grant_notification_permission_help_text))
        Column {
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                content = { Text(stringResource(R.string.grant_permission)) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
