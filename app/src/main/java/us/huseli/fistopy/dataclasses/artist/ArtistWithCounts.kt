package us.huseli.fistopy.dataclasses.artist

import androidx.room.Embedded

data class ArtistWithCounts(
    @Embedded val artist: Artist,
    val trackCount: Int,
    val albumCount: Int,
)
