package us.huseli.fistopy.dataclasses.coverartarchive

import com.google.gson.annotations.SerializedName

@Suppress("unused", "IncorrectFormatting")
enum class CoverArtArchiveImageType {
    @SerializedName("Back") BACK,
    @SerializedName("Booklet") BOOKLET,
    @SerializedName("Bottom") BOTTOM,
    @SerializedName("Front") FRONT,
    @SerializedName("Liner") LINER,
    @SerializedName("Matrix/Runout") MATRIX_RUNOUT,
    @SerializedName("Medium") MEDIUM,
    @SerializedName("Obi") OBI,
    @SerializedName("Other") OTHER,
    @SerializedName("Poster") POSTER,
    @SerializedName("Raw/Unedited") RAW_UNEDITED,
    @SerializedName("Spine") SPINE,
    @SerializedName("Sticker") STICKER,
    @SerializedName("Top") TOP,
    @SerializedName("Track") TRACK,
    @SerializedName("Tray") TRAY,
    @SerializedName("Watermark") WATERMARK,
}
