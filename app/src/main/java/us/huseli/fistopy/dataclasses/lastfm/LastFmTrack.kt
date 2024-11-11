package us.huseli.fistopy.dataclasses.lastfm

import com.google.gson.annotations.SerializedName

data class LastFmTrack(
    @SerializedName("playcount") val playCount: String? = null,
    @SerializedName("userplaycount") val userPlayCount: String? = null,
    @SerializedName("userloved") val userLoved: String? = null,
)
