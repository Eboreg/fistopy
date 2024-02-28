package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.abstr.AbstractAlbumCombo
import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.AlbumTag
import us.huseli.thoucylinder.dataclasses.entities.Tag
import us.huseli.thoucylinder.dataclasses.views.AlbumArtistCredit

@DatabaseView(
    """
    SELECT Album.*,
        SUM(COALESCE(Track_metadata_durationMs, Track_youtubeVideo_durationMs, Track_youtubeVideo_metadata_durationMs)) AS durationMs,
        MIN(Track_year) AS minYear,
        MAX(Track_year) AS maxYear,
        COUNT(Track_trackId) AS trackCount,
        EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NOT NULL)
            AND EXISTS(SELECT * FROM Track WHERE Track_albumId = Album_albumId AND Track_localUri IS NULL)
            AS isPartiallyDownloaded
    FROM Album LEFT JOIN Track ON Album_albumId = Track_albumId
    GROUP BY Album_albumId
    """
)
data class AlbumCombo(
    @Embedded override val album: Album,
    override val durationMs: Long? = null,
    override val minYear: Int? = null,
    override val maxYear: Int? = null,
    override val trackCount: Int = 0,
    override val isPartiallyDownloaded: Boolean = false,
    @Relation(
        entity = Tag::class,
        parentColumn = "Album_albumId",
        entityColumn = "Tag_name",
        associateBy = Junction(
            value = AlbumTag::class,
            parentColumn = "AlbumTag_albumId",
            entityColumn = "AlbumTag_tagName",
        )
    )
    override val tags: List<Tag> = emptyList(),
    @Relation(parentColumn = "Album_albumId", entityColumn = "AlbumArtist_albumId")
    override val artists: List<AlbumArtistCredit> = emptyList(),
) : AbstractAlbumCombo() {
    override fun toString() = album.toString()
}
