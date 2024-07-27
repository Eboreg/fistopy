package us.huseli.fistopy.compose.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import us.huseli.fistopy.compose.FistopyTheme

@Composable
fun OutlinedTextFieldLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.outline,
        style = FistopyTheme.typography.bodySmall,
    )
}