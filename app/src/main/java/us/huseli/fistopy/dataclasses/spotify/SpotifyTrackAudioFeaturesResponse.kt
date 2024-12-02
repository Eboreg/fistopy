package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifyTrackAudioFeaturesResponse(
    @SerializedName("audio_features")
    val audioFeatures: List<SpotifyTrackAudioFeatures?>,
)