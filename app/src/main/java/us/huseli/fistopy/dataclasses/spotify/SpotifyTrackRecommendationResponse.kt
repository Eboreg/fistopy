package us.huseli.fistopy.dataclasses.spotify

data class SpotifyTrackRecommendationResponse(
    val seeds: List<Seed>,
    val tracks: List<SpotifyTrack>,
) {
    data class Seed(
        val afterFilteringSize: Int,
        val afterRelinkingSize: Int,
        val href: String,
        val id: String,
        val initialPoolSize: Int,
        val type: String,
    )
}