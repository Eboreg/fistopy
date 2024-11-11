package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.UnsavedTrackCombo

data class UnsavedAlbumWithTracksCombo(
    override val album: UnsavedAlbum,
    override val artists: List<IAlbumArtistCredit> = emptyList(),
    override val tags: List<Tag> = emptyList(),
    override val trackCombos: List<UnsavedTrackCombo> = emptyList(),
) : IAlbumWithTracksCombo<UnsavedAlbum, UnsavedAlbumWithTracksCombo>, IUnsavedAlbumCombo
