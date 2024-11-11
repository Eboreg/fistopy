package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzArtistSearch(
    val count: Int,
    val offset: Int,
    val artists: List<Artist>,
) {
    data class Artist(
        override val id: String,
        val name: String,
        @SerializedName("sort-name")
        val sortName: String?,
        val aliases: List<Alias>?,
    ) : AbstractMusicBrainzItem() {
        data class Alias(
            @SerializedName("sort-name")
            val sortName: String,
            val name: String,
        )

        fun matches(name: String): Boolean {
            if (name.lowercase() == this.name.lowercase()) return true
            if (name.lowercase() == sortName?.lowercase()) return true
            return aliases?.any {
                it.name.lowercase() == name.lowercase() || it.sortName.lowercase() == name.lowercase()
            } == true
        }
    }
}
