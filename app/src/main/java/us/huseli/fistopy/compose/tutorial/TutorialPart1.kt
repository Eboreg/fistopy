package us.huseli.fistopy.compose.tutorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import us.huseli.fistopy.R
import us.huseli.fistopy.stringResource
import us.huseli.retaintheme.ui.theme.LocalBasicColors

@Composable
fun TutorialPart1() {
    Text(stringResource(R.string.hi_this_is_a_music_player_app))
    Text(stringResource(R.string.what_it_can_do))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        TutorialPart1ListRowPositive(R.string.play_local_music_files)
        TutorialPart1ListRowPositive(R.string.search_and_play_songs_and_albums_on_youtube)
        TutorialPart1ListRowPositive(R.string.match_your_albums_and_play_download)
        TutorialPart1ListRowPositive(R.string.scrobble_to_last_fm)
    }
    Text(stringResource(R.string.what_it_will_not_do))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        TutorialPart1ListRowNegative(R.string.identify_you_with_google)
        TutorialPart1ListRowNegative(R.string.cost_money)
        TutorialPart1ListRowNegative(R.string.display_ads)
        TutorialPart1ListRowNegative(R.string.harvest_your_personal_data)
    }
}

@Composable
fun TutorialPart1ListRow(icon: ImageVector, stringRes: Int, iconColor: Color = LocalContentColor.current) {
    Row {
        Icon(icon, null, modifier = Modifier.padding(horizontal = 5.dp).size(20.dp), tint = iconColor)
        Text(stringResource(stringRes))
    }
}

@Composable
fun TutorialPart1ListRowPositive(stringRes: Int) {
    TutorialPart1ListRow(icon = Icons.Sharp.Check, stringRes = stringRes, iconColor = LocalBasicColors.current.Green)
}

@Composable
fun TutorialPart1ListRowNegative(stringRes: Int) {
    TutorialPart1ListRow(icon = Icons.Sharp.Close, stringRes = stringRes, iconColor = LocalBasicColors.current.Red)
}
