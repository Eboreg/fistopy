package us.huseli.fistopy.dataclasses.radio

import android.content.Context
import androidx.compose.runtime.Immutable
import us.huseli.fistopy.R
import us.huseli.fistopy.enums.RadioType
import us.huseli.fistopy.getUmlautifiedString

@Immutable
data class RadioUiState(
    val type: RadioType,
    val title: String?,
) {
    fun getFullTitle(context: Context): String {
        return if (type == RadioType.LIBRARY) context.getUmlautifiedString(R.string.library_radio)
        else context.getUmlautifiedString(R.string.x_x_radio, title, context.getString(type.stringRes))
    }
}
