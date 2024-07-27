package us.huseli.fistopy.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.spotify.SpotifyTrackAudioFeatures
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class TrackInfoViewModel @Inject constructor(private val repos: Repositories) : AbstractBaseViewModel() {
    private val _isFetchingAudioFeatures = MutableStateFlow(true)

    val isFetchingAudioFeatures = _isFetchingAudioFeatures.asStateFlow()

    fun getAudioFeatures(trackCombo: TrackCombo): StateFlow<SpotifyTrackAudioFeatures?> = flow {
        _isFetchingAudioFeatures.value = true
        if (trackCombo.track.spotifyId != null) {
            emitAll(
                repos.spotify.flowTrackAudioFeatures(trackCombo.track.spotifyId)
                    .onEach { if (it != null) _isFetchingAudioFeatures.value = false }
            )
        } else {
            emit(null)
            _isFetchingAudioFeatures.value = false
        }
    }.stateWhileSubscribed()

    fun getLocalAbsolutePath(track: Track) = repos.track.getLocalAbsolutePath(track)
}
