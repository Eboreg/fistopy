package us.huseli.fistopy.dataclasses.artist

import androidx.compose.runtime.Immutable
import us.huseli.fistopy.dataclasses.MediaStoreImage

@Immutable
data class UnsavedAlbumArtistCredit(
    override val albumId: String,
    override val name: String,
    override val spotifyId: String? = null,
    override val musicBrainzId: String? = null,
    override val image: MediaStoreImage? = null,
    override val joinPhrase: String = "/",
    override val position: Int = 0,
) : IAlbumArtistCredit {
    override fun withAlbumId(albumId: String): UnsavedAlbumArtistCredit = copy(albumId = albumId)

    companion object {
        fun fromArtistCredits(artistCredits: Iterable<IArtistCredit>, albumId: String): List<UnsavedAlbumArtistCredit> {
            return artistCredits.map {
                UnsavedAlbumArtistCredit(
                    albumId = albumId,
                    name = it.name,
                    spotifyId = it.spotifyId,
                    musicBrainzId = it.musicBrainzId,
                    image = it.image,
                    joinPhrase = it.joinPhrase,
                    position = it.position,
                )
            }
        }
    }
}
