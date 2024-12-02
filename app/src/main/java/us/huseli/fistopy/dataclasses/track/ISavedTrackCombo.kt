package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.TrackArtistCredit

interface ISavedTrackCombo<T : ISavedTrackCombo<T>> : ITrackCombo<T> {
    override val trackArtists: List<TrackArtistCredit>
    override val albumArtists: List<AlbumArtistCredit>
    override val album: Album?
}
