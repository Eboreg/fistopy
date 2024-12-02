package us.huseli.fistopy.dataclasses.spotify

data class SpotifyResponse<T>(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<T>,
)
