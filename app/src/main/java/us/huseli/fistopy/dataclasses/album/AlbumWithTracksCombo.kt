package us.huseli.fistopy.dataclasses.album

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.tag.AlbumTag
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.TrackCombo

data class AlbumWithTracksCombo(
    @Embedded override val album: Album,
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
    @Relation(parentColumn = "Album_albumId", entityColumn = "Track_albumId")
    override val trackCombos: List<TrackCombo> = emptyList<TrackCombo>().let { combos ->
        combos.map { it.copy(albumArtists = artists) }
    },
) : IAlbumWithTracksCombo<Album, AlbumWithTracksCombo>, ISavedAlbumCombo
