package us.huseli.fistopy.interfaces

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.IUnsavedAlbumCombo
import us.huseli.fistopy.dataclasses.toMediaStoreImage
import us.huseli.fistopy.enums.AlbumType
import kotlin.time.Duration

interface IExternalAlbum : IStringIdItem {
    override val id: String
    val title: String
    val artistName: String?
    val thumbnailUrl: String?
    val trackCount: Int?
    val year: Int?
    val duration: Duration?
    val playCount: Int?
    val albumType: AlbumType?

    fun getMediaStoreImage(): MediaStoreImage? = thumbnailUrl?.toMediaStoreImage()
    fun toAlbumCombo(isLocal: Boolean, isInLibrary: Boolean, albumId: String? = null): IUnsavedAlbumCombo
}
