package us.huseli.fistopy.compose.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedSection(
    visible: Boolean,
    tonalElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(500)),
        exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(500)),
    ) {
        Surface(
            color = BottomAppBarDefaults.containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                content = content,
            )
        }
    }
}
