package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.interfaces.IStringIdItem

interface IExternalTrackComboProducer<T : IStringIdItem> {
    fun toTrackCombo(
        isInLibrary: Boolean = false,
        album: UnsavedAlbum? = null,
        albumArtists: List<IAlbumArtistCredit>? = null,
        albumPosition: Int? = null,
    ): ExternalTrackCombo<T>
}
