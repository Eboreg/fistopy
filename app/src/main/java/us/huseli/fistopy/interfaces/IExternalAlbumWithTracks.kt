package us.huseli.fistopy.interfaces

import us.huseli.fistopy.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.fistopy.dataclasses.album.UnsavedAlbumWithTracksCombo

interface IExternalAlbumWithTracks : IExternalAlbum {
    fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String? = null,
    ): UnsavedAlbumWithTracksCombo

    override fun toAlbumCombo(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String?,
    ): IUnsavedAlbumCombo =
        toAlbumWithTracks(isLocal = isLocal, isInLibrary = isInLibrary, albumId = albumId)
}
