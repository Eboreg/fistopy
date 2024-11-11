package us.huseli.fistopy.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit

data class LastFmArtist(
    val mbid: String,
    @SerializedName("playcount") val playCount: String? = null,
    val name: String,
    val image: List<LastFmImage>? = null,
) {
    fun toNativeAlbumArtist(albumId: String) =
        UnsavedAlbumArtistCredit(name = name, albumId = albumId, musicBrainzId = mbid)
}
