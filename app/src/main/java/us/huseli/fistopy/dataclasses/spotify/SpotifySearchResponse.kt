package us.huseli.fistopy.dataclasses.spotify

data class SpotifySearchResponse(
    val albums: SpotifyResponse<SpotifySimplifiedAlbum>?,
    val artists: SpotifyResponse<SpotifyArtist>?,
    val tracks: SpotifyResponse<SpotifyTrack>?,
)