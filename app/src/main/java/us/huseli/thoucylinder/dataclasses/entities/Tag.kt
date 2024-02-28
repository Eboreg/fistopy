package us.huseli.thoucylinder.dataclasses.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity
data class Tag(
    @ColumnInfo("Tag_name") @PrimaryKey val name: String,
    @ColumnInfo("Tag_isMusicBrainzGenre") val isMusicBrainzGenre: Boolean = false,
) : Parcelable

fun Iterable<Tag>.toAlbumTags(albumId: UUID) = map { AlbumTag(albumId, it.name) }