package us.huseli.fistopy.dataclasses.album

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.youtube.YoutubePlaylist
import us.huseli.fistopy.enums.AlbumType
import java.util.UUID

@Entity(indices = [Index("Album_isInLibrary")])
@Parcelize
@Immutable
data class Album(
    @PrimaryKey @ColumnInfo("Album_albumId") override val albumId: String = UUID.randomUUID().toString(),
    @ColumnInfo("Album_title") override val title: String,
    @ColumnInfo("Album_isInLibrary") override val isInLibrary: Boolean,
    @ColumnInfo("Album_isLocal") override val isLocal: Boolean,
    @ColumnInfo("Album_year") override val year: Int? = null,
    @ColumnInfo("Album_isHidden") override val isHidden: Boolean = false,
    @ColumnInfo("Album_musicBrainzReleaseId") override val musicBrainzReleaseId: String? = null,
    @ColumnInfo("Album_musicBrainzReleaseGroupId") override val musicBrainzReleaseGroupId: String? = null,
    @ColumnInfo("Album_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("Album_albumType") override val albumType: AlbumType? = null,
    @ColumnInfo("Album_trackCount") override val trackCount: Int? = null,
    @Embedded("Album_youtubePlaylist_") override val youtubePlaylist: YoutubePlaylist? = null,
    @Embedded("Album_albumArt_") override val albumArt: MediaStoreImage? = null,
) : Parcelable, IAlbum {
    override val isSaved: Boolean
        get() = true

    override fun asSavedAlbum() = this

    override fun toString(): String = title
}
