package us.huseli.fistopy.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit

data class LastFmArtist(
    val mbid: String,
    @SerializedName("playcount") val playCount: String? = null,
    val name: String,
    val image: List<LastFmImage>? = null,
) {
    fun toNativeArtist() = UnsavedArtistCredit(name = name, musicBrainzId = mbid, image = image?.toMediaStoreImage())
}
