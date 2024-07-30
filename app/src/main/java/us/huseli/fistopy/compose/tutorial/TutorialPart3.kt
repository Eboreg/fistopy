package us.huseli.fistopy.compose.tutorial

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import us.huseli.fistopy.Constants.LASTFM_AUTH_URL
import us.huseli.fistopy.R
import us.huseli.fistopy.TutorialDestination
import us.huseli.fistopy.stringResource
import us.huseli.fistopy.viewmodels.TutorialViewModel

@Composable
fun TutorialPart3(viewModel: TutorialViewModel = hiltViewModel()) {
    val uriHandler = LocalUriHandler.current

    Text(stringResource(R.string.if_you_want_to_scrobble_your_listened_tracks_to_last_fm))
    Text(stringResource(R.string.you_can_always_do_this_later_in_the_settings))
    Column {
        Button(
            onClick = {
                viewModel.setStartDestination(TutorialDestination.route)
                uriHandler.openUri(LASTFM_AUTH_URL)
                viewModel.gotoNextPage()
            },
            content = { Text(stringResource(R.string.authorize_with_last_fm)) },
            modifier = Modifier.align(Alignment.End),
            shape = MaterialTheme.shapes.small,
        )
    }
}
