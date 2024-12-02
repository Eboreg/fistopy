package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName

data class SpotifySavedAlbumObject(
    @SerializedName("added_at")
    val addedAt: String,
    val album: SpotifyAlbum,
)