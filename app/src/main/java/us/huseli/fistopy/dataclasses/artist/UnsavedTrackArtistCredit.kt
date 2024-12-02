package us.huseli.fistopy.dataclasses.artist

import androidx.compose.runtime.Immutable
import us.huseli.fistopy.dataclasses.MediaStoreImage

@Immutable
data class UnsavedTrackArtistCredit(
    override val name: String,
    override val spotifyId: String? = null,
    override val musicBrainzId: String? = null,
    override val image: MediaStoreImage? = null,
    override val joinPhrase: String = "/",
    override val position: Int = 0,
    override val trackId: String,
) : ITrackArtistCredit {
    override fun withTrackId(trackId: String): UnsavedTrackArtistCredit = copy(trackId = trackId)

    companion object {
        fun fromArtistCredits(artistCredits: Iterable<IArtistCredit>, trackId: String) = artistCredits.map {
            UnsavedTrackArtistCredit(
                name = it.name,
                spotifyId = it.spotifyId,
                musicBrainzId = it.musicBrainzId,
                image = it.image,
                joinPhrase = it.joinPhrase,
                position = it.position,
                trackId = trackId,
            )
        }
    }
}
