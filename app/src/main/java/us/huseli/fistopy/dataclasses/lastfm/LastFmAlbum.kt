package us.huseli.fistopy.dataclasses.lastfm

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.IStringIdItem

data class LastFmAlbum(
    val mbid: String,
    val name: String,
    val artist: LastFmArtist,
    val image: List<LastFmImage>,
    @SerializedName("playcount") val playCount: String?,
) : IExternalAlbumWithTracksProducer<LastFmAlbum>, IStringIdItem {
    override val id: String
        get() = mbid

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String,
    ): ExternalAlbumWithTracksCombo<LastFmAlbum> {
        val builder = ExternalAlbumWithTracksCombo.Builder(
            album = UnsavedAlbum(
                albumArt = image.toMediaStoreImage(),
                albumType = if (artist.name.lowercase() == "various artists") AlbumType.COMPILATION else null,
                musicBrainzReleaseId = mbid,
                title = name,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                albumId = albumId,
            ),
            externalData = this,
            artists = listOf(artist.toNativeArtist()),
            playCount = playCount?.toIntOrNull(),
        )

        return builder.build()
    }

    override fun toString(): String = artist.name.takeIf { it.isNotEmpty() }?.let { "$it - $name" } ?: name
}
