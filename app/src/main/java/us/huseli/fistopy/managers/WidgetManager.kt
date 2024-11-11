package us.huseli.fistopy.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.glance.ImageProvider
import androidx.glance.appwidget.updateAll
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.Constants.PREF_WIDGET_BUTTONS
import us.huseli.fistopy.R
import us.huseli.fistopy.getAverageColor
import us.huseli.fistopy.getBitmap
import us.huseli.fistopy.getUmlautifiedString
import us.huseli.fistopy.repositories.Repositories
import us.huseli.fistopy.widget.AppWidget
import us.huseli.fistopy.widget.WidgetButton
import us.huseli.retaintheme.extensions.square
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetManager @Inject constructor(
    private val repos: Repositories,
    @ApplicationContext private val context: Context,
) : AbstractScopeHolder(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _buttons = MutableStateFlow(
        WidgetButton.fromNames(
            preferences.getStringSet(PREF_WIDGET_BUTTONS, WidgetButton.defaultNames) ?: WidgetButton.defaultNames
        )
    )
    private val widgetSize = MutableStateFlow<DpSize?>(null)
    private val albumArtSize = widgetSize.filterNotNull()
        .map { ((it.height - 20.dp).value * context.resources.displayMetrics.density).fastRoundToInt() }
    private val defaultAlbumArt = ImageProvider(R.drawable.splashscreen_icon)
    private val defaultTrackString = context.getUmlautifiedString(R.string.no_track_playing)
    private val albumArtBitmap = combine(albumArtSize, repos.player.currentCombo) { size, combo ->
        combo?.let { context.getBitmap(it, size)?.square() }
    }

    val albumArt = albumArtBitmap.map { bitmap ->
        bitmap?.let { ImageProvider(it) } ?: defaultAlbumArt
    }.distinctUntilChanged().stateWhileSubscribed(defaultAlbumArt)
    val albumArtAverageColor = albumArtBitmap.map { bitmap ->
        bitmap?.asImageBitmap()?.getAverageColor()?.copy(alpha = 0.3f)
    }.stateWhileSubscribed()
    val buttons = _buttons.asStateFlow()
    val canGotoNext = repos.player.canGotoNext
    val canGotoPrevious = repos.player.canGotoPrevious
    val canPlay = repos.player.canPlay.stateWhileSubscribed(false)
    val currentTrackString: StateFlow<String> = repos.player.currentCombo
        .map { combo ->
            combo?.let { listOfNotNull(it.artistString, it.track.title) }?.joinToString(" • ") ?: defaultTrackString
        }
        .distinctUntilChanged()
        .stateWhileSubscribed(defaultTrackString)
    val isPlaying = repos.player.isPlaying.stateWhileSubscribed(false)
    val isRepeatEnabled = repos.player.isRepeatEnabled
    val isShuffleEnabled = repos.player.isShuffleEnabled

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun playOrPauseCurrent() = repos.player.playOrPauseCurrent()

    fun seekBack() = repos.player.seekBack()

    fun seekForward() = repos.player.seekForward()

    fun setWidgetSize(value: DpSize) {
        widgetSize.value = value
    }

    fun skipToNext() = repos.player.skipToNext()

    fun skipToStartOrPrevious() = repos.player.skipToStartOrPrevious()

    fun toggleRepeat() = repos.player.toggleRepeat()

    fun toggleShuffle() = repos.player.toggleShuffle()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_WIDGET_BUTTONS) {
            _buttons.value = WidgetButton.fromNames(
                preferences.getStringSet(PREF_WIDGET_BUTTONS, WidgetButton.defaultNames) ?: WidgetButton.defaultNames
            )
            launchOnIOThread { AppWidget().updateAll(context) }
        }
    }
}
