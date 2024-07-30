package us.huseli.fistopy.compose.tutorial

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.fistopy.R
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.SettingsViewModel

@Composable
fun TutorialPart2(onFinish: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val selectDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setAutoImportLocalMusic(true)
            viewModel.setLocalMusicUri(uri)
            viewModel.importNewLocalAlbums()
            onFinish()
        }
    }

    Text(stringResource(R.string.if_you_want_to_auto_import_local_music))
    Text(stringResource(R.string.you_can_always_change_this_later_in_the_settings))
    Column {
        Button(
            onClick = { selectDirlauncher.launch(null) },
            content = { Text(stringResource(R.string.select_directory)) },
            modifier = Modifier.align(Alignment.End),
            shape = MaterialTheme.shapes.small,
        )
    }
}
