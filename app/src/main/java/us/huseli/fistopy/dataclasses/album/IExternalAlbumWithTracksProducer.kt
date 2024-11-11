package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.interfaces.IStringIdItem
import java.util.UUID

interface IExternalAlbumWithTracksProducer<T : IStringIdItem> {
    fun toAlbumWithTracks(
        isLocal: Boolean = false,
        isInLibrary: Boolean = false,
        albumId: String = UUID.randomUUID().toString(),
    ): ExternalAlbumWithTracksCombo<T>
}
