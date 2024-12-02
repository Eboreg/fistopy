package us.huseli.fistopy.dataclasses.youtube

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.interfaces.IStringIdItem

@Immutable
data class YoutubePlaylistCombo(val playlist: YoutubePlaylist, val videos: ImmutableList<YoutubeVideo>) :
    IExternalAlbumWithTracksProducer<YoutubePlaylistCombo>, IStringIdItem {
    override val id: String
        get() = playlist.id

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String,
    ): ExternalAlbumWithTracksCombo<YoutubePlaylistCombo> {
        val builder = ExternalAlbumWithTracksCombo.Builder(
            album = UnsavedAlbum(
                albumArt = playlist.getMediaStoreImage(),
                albumType = playlist.albumType,
                title = playlist.title,
                trackCount = videos.size,
                year = null,
                youtubePlaylist = playlist,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                albumId = albumId,
            ),
            externalData = this,
            tracks = videos.stripTitleCommons(),
        )

        playlist.artist?.takeIf { it.lowercase() != "various artists" }?.also { builder.setArtist(it) }
        return builder.build()
    }
}
