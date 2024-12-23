package us.huseli.fistopy.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.fistopy.Umlautify
import us.huseli.fistopy.dataclasses.spotify.SpotifyUserProfile
import us.huseli.fistopy.enums.Region
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    private val _spotifyUserProfile = MutableStateFlow<SpotifyUserProfile?>(null)

    val autoImportLocalMusic: StateFlow<Boolean?> = repos.settings.autoImportLocalMusic
    val lastFmIsAuthenticated: Flow<Boolean> = repos.lastFm.isAuthenticated
    val lastFmScrobble: StateFlow<Boolean> = repos.lastFm.scrobble
    val lastFmUsername: StateFlow<String?> = repos.lastFm.username
    val localMusicUri: StateFlow<Uri?> = repos.settings.localMusicUri
    val region: StateFlow<Region> = repos.settings.region
    val spotifyUserProfile: StateFlow<SpotifyUserProfile?> = _spotifyUserProfile.asStateFlow()
    val umlautify: StateFlow<Boolean> = Umlautify.isEnabled

    init {
        launchOnIOThread {
            _spotifyUserProfile.value = repos.spotify.getUserProfile()
        }
    }

    fun disableLastFmScrobble() = repos.lastFm.setScrobble(false)

    fun enableLastFmScrobble() = repos.lastFm.setScrobble(true)

    fun forgetSpotifyProfile() {
        repos.spotify.unauthorize()
        _spotifyUserProfile.value = null
    }

    fun importNewLocalAlbums() = launchOnIOThread { managers.library.importNewLocalAlbums() }

    fun setAutoImportLocalMusic(value: Boolean) = repos.settings.setAutoImportLocalMusic(value)

    fun setLastFmUsername(value: String?) = repos.lastFm.setUsername(value)

    fun setLocalMusicUri(value: Uri?) = repos.settings.setLocalMusicUri(value)

    fun setRegion(value: Region) = repos.settings.setRegion(value)

    fun setStartDestination(value: String) = repos.settings.setStartDestination(value)

    fun setUmlautify(value: Boolean) = repos.settings.setUmlautify(value)

    fun unhideLocalAlbums() = launchOnIOThread {
        repos.album.unhideLocalAlbums()
        repos.message.onUnhideLocalAlbums()
    }
}
