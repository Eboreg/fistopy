package us.huseli.fistopy.dataclasses.coverartarchive

data class CoverArtArchiveResponse(
    val images: List<CoverArtArchiveImage>,
    val release: String,
)
