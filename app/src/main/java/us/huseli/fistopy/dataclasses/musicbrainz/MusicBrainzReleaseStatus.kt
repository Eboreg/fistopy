package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official")
    OFFICIAL,

    @SerializedName("Promotion")
    PROMOTION,

    @SerializedName("Bootleg")
    BOOTLEG,

    @SerializedName("Pseudo-release")
    PSEUDO_RELEASE,

    @SerializedName("Withdrawn")
    WITHDRAWN,

    @SerializedName("Cancelled")
    CANCELLED,
}