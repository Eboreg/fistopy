package us.huseli.fistopy.dataclasses.coverartarchive

import com.google.gson.annotations.SerializedName

data class CoverArtArchiveImageThumbnails(
    @SerializedName("250") val thumb250: String,
    @SerializedName("500") val thumb500: String,
    @SerializedName("1200") val thumb1200: String,
)
