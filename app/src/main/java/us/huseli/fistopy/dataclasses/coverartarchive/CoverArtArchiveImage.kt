package us.huseli.fistopy.dataclasses.coverartarchive

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.toMediaStoreImage

data class CoverArtArchiveImage(
    val approved: Boolean,
    val back: Boolean,
    val comment: String,
    val edit: Int,
    val front: Boolean,
    val id: Long,
    val image: String,
    val thumbnails: CoverArtArchiveImageThumbnails,
    val types: List<CoverArtArchiveImageType>,
) {
    fun toMediaStoreImage(): MediaStoreImage = image.toMediaStoreImage(thumbnails.thumb250)
}
