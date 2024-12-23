package us.huseli.fistopy.dataclasses.album

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit

@DatabaseView(
    """
    SELECT Album.*,
        MIN(Track_year) AS AlbumCombo_minYear,
        MAX(Track_year) AS AlbumCombo_maxYear,
        EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NOT NULL)
            AND EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL)
            AS AlbumCombo_isPartiallyDownloaded,
        (SELECT COUNT(Track_trackId) FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL AND Track_youtubeVideo_metadata_url IS NULL)
            AS AlbumCombo_unplayableTrackCount,
        EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL AND Track_youtubeVideo_metadata_url IS NOT NULL)
            AS AlbumCombo_isDownloadable
    FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId
    GROUP BY Album_albumId
    """
)
@Immutable
data class AlbumCombo(
    @Embedded override val album: Album,
    @ColumnInfo("AlbumCombo_minYear") override val minYear: Int?,
    @ColumnInfo("AlbumCombo_maxYear") override val maxYear: Int?,
    @ColumnInfo("AlbumCombo_isPartiallyDownloaded") override val isPartiallyDownloaded: Boolean,
    @Relation(parentColumn = "Album_albumId", entityColumn = "AlbumArtist_albumId")
    override val artists: List<AlbumArtistCredit>,
    @ColumnInfo("AlbumCombo_unplayableTrackCount") override val unplayableTrackCount: Int,
    @ColumnInfo("AlbumCombo_isDownloadable") override val isDownloadable: Boolean,
) : IAlbumCombo<Album>
