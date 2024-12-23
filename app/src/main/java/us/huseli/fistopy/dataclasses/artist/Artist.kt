package us.huseli.fistopy.dataclasses.artist

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.fistopy.dataclasses.MediaStoreImage
import java.util.UUID

@Parcelize
@Entity
@Immutable
data class Artist(
    @ColumnInfo("Artist_name") override val name: String,
    @ColumnInfo("Artist_id") @PrimaryKey override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Artist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("Artist_musicBrainzId") override val musicBrainzId: String? = null,
    @Embedded("Artist_image_") override val image: MediaStoreImage? = null,
) : Parcelable, IArtist, ISavedArtist {
    fun updateWith(other: IArtist) = copy(
        spotifyId = spotifyId ?: other.spotifyId,
        musicBrainzId = musicBrainzId ?: other.musicBrainzId,
        image = image ?: other.image,
    )
}
