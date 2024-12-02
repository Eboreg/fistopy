package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifySimplifiedTrack(
    override val artists: List<SpotifySimplifiedArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
) : AbstractSpotifyTrack<SpotifySimplifiedArtist, SpotifySimplifiedTrack>()