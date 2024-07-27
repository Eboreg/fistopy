package us.huseli.fistopy.dataclasses.spotify

data class SpotifyTrackRecommendations(
    val tracks: List<SpotifyTrack>,
    val requestedTracks: Int,
    val foundTracks: Int = tracks.size,
    val hasMore: Boolean = foundTracks >= requestedTracks,
)
