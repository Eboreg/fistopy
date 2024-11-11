package us.huseli.fistopy.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.toMediaStoreImage
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.IStringIdItem

@Parcelize
@Immutable
data class YoutubePlaylist(
    override val id: String,
    val title: String,
    val artist: String? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
    val videoCount: Int = 0,
) : Parcelable, IExternalAlbumWithTracksProducer<YoutubePlaylist>, IStringIdItem {
    val albumType: AlbumType?
        get() = if (artist?.lowercase() == "various artists") AlbumType.COMPILATION else null

    val thumbnailUrl: String?
        get() = fullImage?.url ?: thumbnail?.url

    fun getMediaStoreImage(): MediaStoreImage? =
        (fullImage?.url ?: thumbnail?.url)?.toMediaStoreImage(thumbnail?.url)

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String,
    ): ExternalAlbumWithTracksCombo<YoutubePlaylist> {
        val album = UnsavedAlbum(
            albumArt = getMediaStoreImage(),
            albumId = albumId,
            albumType = albumType,
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            title = title,
            trackCount = videoCount,
            youtubePlaylist = this,
        )
        val albumArtists = artist
            ?.takeIf { it.lowercase() != "various artists" }
            ?.let { listOf(UnsavedAlbumArtistCredit(name = it, albumId = album.albumId)) }
            ?: emptyList()

        return ExternalAlbumWithTracksCombo(
            externalData = this,
            album = album,
            artists = albumArtists,
            trackCombos = emptyList(),
            tags = emptyList(),
        )
    }

    override fun toString() = "${artist?.let { "$it - $title" } ?: title} ($videoCount videos)"
}
