package us.huseli.fistopy.dataclasses.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.fistopy.dataclasses.MediaStoreImage
import java.util.UUID

@DatabaseView(
    """
    SELECT AlbumArtist.*,
        Artist_name AS AlbumArtist_name,
        Artist_spotifyId AS AlbumArtist_spotifyId,
        Artist_musicBrainzId AS AlbumArtist_musicBrainzId,
        Artist_image_fullUriString AS AlbumArtist_image_fullUriString,
        Artist_image_thumbnailUriString AS AlbumArtist_image_thumbnailUriString
    FROM AlbumArtist JOIN Artist ON AlbumArtist_artistId = Artist_id
    ORDER BY AlbumArtist_position
    """
)
@Immutable
data class AlbumArtistCredit(
    @ColumnInfo("AlbumArtist_albumId") override val albumId: String,
    @ColumnInfo("AlbumArtist_artistId") override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("AlbumArtist_name") override val name: String,
    @ColumnInfo("AlbumArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("AlbumArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("AlbumArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("AlbumArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("AlbumArtist_position") override val position: Int = 0,
) : IAlbumArtistCredit, ISavedArtist {
    override fun withAlbumId(albumId: String): AlbumArtistCredit = copy(albumId = albumId)
}

fun Iterable<AlbumArtistCredit>.toAlbumArtists(): List<AlbumArtist> = map {
    AlbumArtist(
        albumId = it.albumId,
        artistId = it.artistId,
        joinPhrase = it.joinPhrase,
        position = it.position,
    )
}
