package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzLabelInfo(
    val label: Label,
    @SerializedName("catalog-number")
    val catalogNumber: String,
) {
    data class Label(
        override val id: String,
        val type: MusicBrainzLabelType,
        val name: String,
    ) : AbstractMusicBrainzItem()
}
