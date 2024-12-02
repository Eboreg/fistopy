package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

@Suppress("IncorrectFormatting", "unused")
enum class MusicBrainzLabelType {
    @SerializedName("Imprint") IMPRINT,
    @SerializedName("Original Production") ORIGINAL_PRODUCTION,
    @SerializedName("Bootleg Production") BOOTLEG_PRODUCTION,
    @SerializedName("Reissue Production") REISSUE_PRODUCTION,
    @SerializedName("Distributor") DISTRIBUTOR,
    @SerializedName("Holding") HOLDING,
    @SerializedName("Rights Society") RIGHTS_SOCIETY,
}