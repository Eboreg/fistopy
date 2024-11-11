package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.youtube.YoutubePlaylist
import us.huseli.fistopy.enums.AlbumType
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
}