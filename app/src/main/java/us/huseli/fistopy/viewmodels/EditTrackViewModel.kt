package us.huseli.fistopy.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retaintheme.extensions.launchOnIOThread
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import javax.inject.Inject

@HiltViewModel
class EditTrackViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : AbstractBaseViewModel() {
    suspend fun getArtistNameSuggestions(name: String, limit: Int = 10) =
        repos.artist.getArtistNameSuggestions(name, limit)

    fun updateTrack(
        trackId: String,
        title: String,
        year: Int?,
        albumPosition: Int?,
        discNumber: Int?,
        artistNames: Collection<String>,
    ) {
        launchOnIOThread {
            managers.library.updateTrack(
                trackId = trackId,
                title = title,
                year = year,
                albumPosition = albumPosition,
                discNumber = discNumber,
                artistNames = artistNames,
            )
        }
    }
}
