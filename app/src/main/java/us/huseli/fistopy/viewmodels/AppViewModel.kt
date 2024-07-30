package us.huseli.fistopy.viewmodels

import android.content.Context
import android.content.Intent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.Constants.PREF_APP_START_COUNT
import us.huseli.fistopy.ImportDestination
import us.huseli.fistopy.Umlautify
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
    @ApplicationContext context: Context,
) : DownloadsViewModel(repos, managers) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val currentTrackExists =
        repos.player.currentCombo.map { it != null }.distinctUntilChanged().stateWhileSubscribed(false)
    val contentSize: StateFlow<Size> = repos.settings.contentSize
    val umlautifier = Umlautify.umlautifier.stateWhileSubscribed(Umlautify.disabledUmlautifier)
    val albumImportProgress = managers.external.albumImportProgress
        .map { if (it.isActive) it.progress else null }
        .stateWhileSubscribed()

    fun addAlbumToLibrary(
        albumId: String,
        onGotoAlbumClick: (String) -> Unit,
        onGotoLibraryClick: (() -> Unit)? = null,
    ) = launchOnIOThread {
        managers.library.addAlbumsToLibrary(
            albumIds = listOf(albumId),
            onGotoAlbumClick = onGotoAlbumClick,
            onGotoLibraryClick = onGotoLibraryClick,
        )
    }

    fun clearCustomStartDestination() = repos.settings.clearCustomStartDestination()

    fun enqueueAlbum(albumId: String) = managers.player.enqueueAlbums(listOf(albumId))

    fun enqueueArtist(artistId: String) = managers.player.enqueueArtist(artistId)

    fun enqueueTrack(trackId: String) = managers.player.enqueueTracks(listOf(trackId))

    fun getStartDestination(): String = repos.settings.getStartDestination()

    fun handleLastFmIntent(intent: Intent) = launchOnIOThread {
        intent.data?.getQueryParameter("token")?.also { authToken ->
            try {
                repos.lastFm.getSession(authToken)
            } catch (e: Exception) {
                repos.lastFm.setScrobble(false)
                repos.message.onLastFmAuthError(error = e)
                logError("handleIntent: $e", e)
            }
        }
    }

    fun handleSpotifyIntent(intent: Intent) = launchOnIOThread {
        try {
            repos.spotify.oauth2PKCE.handleIntent(intent)
        } catch (e: Exception) {
            logError("handleIntent: $e", e)
            repos.message.onSpotifyAuthError(error = e)
        }
        repos.settings.setStartDestination(ImportDestination.route)
    }

    fun incrementAppStartCount() {
        val value = preferences.getInt(PREF_APP_START_COUNT, 0) + 1
        preferences.edit().putInt(PREF_APP_START_COUNT, value).apply()
    }

    fun playAlbum(albumId: String) = managers.player.playAlbum(albumId)

    fun playArtist(artistId: String) = managers.player.playArtist(artistId)

    fun playTrack(trackId: String) = managers.player.playTrack(trackId)

    fun setContentSize(size: Size) = repos.settings.setContentSize(size)

    fun setScreenSize(dpSize: DpSize, size: Size) = repos.settings.setScreenSize(dpSize, size)

    fun startAlbumRadio(albumId: String) = managers.radio.startAlbumRadio(albumId)

    fun startArtistRadio(artistId: String) = managers.radio.startArtistRadio(artistId)

    fun startTrackRadio(trackId: String) = managers.radio.startTrackRadio(trackId)
}
