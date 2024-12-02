package us.huseli.fistopy.dataclasses.spotify

data class SpotifySimplifiedArtist(
    override val id: String,
    override val name: String,
) : AbstractSpotifyItem(), ISpotifyArtist
