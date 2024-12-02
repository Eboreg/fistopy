package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifySimplifiedAlbum(
    @SerializedName("album_type")
    override val spotifyAlbumType: SpotifyAlbumType?,
    override val artists: List<SpotifySimplifiedArtist>,
    override val id: String,
    override val images: List<SpotifyImage>,
    override val name: String,
    @SerializedName("release_date")
    override val releaseDate: String,
    @SerializedName("total_tracks")
    override val totalTracks: Int,
) : AbstractSpotifyAlbum<SpotifySimplifiedAlbum>()
