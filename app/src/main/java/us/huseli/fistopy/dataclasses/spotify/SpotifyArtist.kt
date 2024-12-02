package us.huseli.fistopy.dataclasses.spotify

import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit

data class SpotifyArtist(
    override val id: String,
    override val name: String,
    val images: List<SpotifyImage>,
    val popularity: Int?,
) : AbstractSpotifyItem(), ISpotifyArtist {
    override fun toNativeArtist(position: Int) = UnsavedArtistCredit(
        name = name,
        spotifyId = id,
        image = images.toMediaStoreImage(),
        position = position,
    )
}
