package us.huseli.fistopy.dataclasses.album

import java.util.UUID

interface IAlbumProducer {
    fun toAlbum(
        isLocal: Boolean = false,
        isInLibrary: Boolean = false,
        albumId: String = UUID.randomUUID().toString(),
    ): UnsavedAlbum
}
