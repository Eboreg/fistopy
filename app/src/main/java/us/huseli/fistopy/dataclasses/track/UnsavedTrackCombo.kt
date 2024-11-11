package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.ITrackArtistCredit

data class UnsavedTrackCombo(
    override val album: UnsavedAlbum? = null,
    override val albumArtists: List<IAlbumArtistCredit> = emptyList(),
    override val track: Track,
    override val trackArtists: List<ITrackArtistCredit> = emptyList(),
) : ITrackCombo<UnsavedTrackCombo> {
    override fun withTrack(track: Track) = copy(track = track)
}
