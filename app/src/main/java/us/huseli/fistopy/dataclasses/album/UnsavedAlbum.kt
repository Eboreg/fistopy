package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.youtube.YoutubePlaylist
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.enums.OnConflictStrategy
import java.util.UUID

data class UnsavedAlbum(
    override val albumArt: MediaStoreImage? = null,
    override val albumId: String = UUID.randomUUID().toString(),
    override val albumType: AlbumType? = null,
    override val isHidden: Boolean = false,
    override val isInLibrary: Boolean = false,
    override val isLocal: Boolean = false,
    override val isSaved: Boolean = false,
    override val musicBrainzReleaseGroupId: String? = null,
    override val musicBrainzReleaseId: String? = null,
    override val spotifyId: String? = null,
    override val title: String,
    override val trackCount: Int? = null,
    override val year: Int? = null,
    override val youtubePlaylist: YoutubePlaylist? = null,
) : IAlbum {
    override fun asUnsavedAlbum(): UnsavedAlbum = this

    fun mergeWith(other: IAlbum, onConflictStrategy: OnConflictStrategy): UnsavedAlbum {
        return when (onConflictStrategy) {
            OnConflictStrategy.USE_THIS -> copy(
                albumArt = albumArt ?: other.albumArt,
                albumType = albumType ?: other.albumType,
                musicBrainzReleaseId = musicBrainzReleaseId ?: other.musicBrainzReleaseId,
                musicBrainzReleaseGroupId = musicBrainzReleaseGroupId ?: other.musicBrainzReleaseGroupId,
                spotifyId = spotifyId ?: other.spotifyId,
                youtubePlaylist = youtubePlaylist ?: other.youtubePlaylist,
            )
            OnConflictStrategy.USE_OTHER -> copy(
                albumArt = other.albumArt ?: albumArt,
                albumType = other.albumType ?: albumType,
                musicBrainzReleaseGroupId = other.musicBrainzReleaseGroupId ?: musicBrainzReleaseGroupId,
                musicBrainzReleaseId = other.musicBrainzReleaseId ?: musicBrainzReleaseId,
                spotifyId = other.spotifyId ?: spotifyId,
                youtubePlaylist = other.youtubePlaylist ?: youtubePlaylist,
            )
        }
    }
}
