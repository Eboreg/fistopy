package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.ITrackArtistCredit
import us.huseli.fistopy.interfaces.IStringIdItem

data class ExternalTrackCombo<T : IStringIdItem>(
    val externalData: T,
    override val album: UnsavedAlbum?,
    override val track: Track,
    override val trackArtists: List<ITrackArtistCredit>,
    override val albumArtists: List<IAlbumArtistCredit>,
) : ITrackCombo<ExternalTrackCombo<T>> {
    override fun withTrack(track: Track) = copy(track = track)
}
