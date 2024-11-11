package us.huseli.fistopy.dataclasses.album

import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.youtube.YoutubePlaylist
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.IStringIdItem
import kotlin.math.absoluteValue

interface IAlbum : IAlbumArtOwner, IHasMusicBrainzIds, IStringIdItem {
    val albumArt: MediaStoreImage?
    val albumId: String
    val albumType: AlbumType?
    val isHidden: Boolean
    val isInLibrary: Boolean
    val isLocal: Boolean
    val isSaved: Boolean
    val spotifyId: String?
    val title: String
    val trackCount: Int?
    val year: Int?
    val youtubePlaylist: YoutubePlaylist?

    override val id: String
        get() = albumId

    override val fullImageUrl: String?
        get() = albumArt?.fullUriString ?: youtubePlaylist?.fullImage?.url

    val spotifyWebUrl: String?
        get() = spotifyId?.let { "https://open.spotify.com/album/${it}" }

    override val thumbnailUrl: String?
        get() = albumArt?.thumbnailUriString ?: youtubePlaylist?.thumbnailUrl

    val youtubeWebUrl: String?
        get() = youtubePlaylist?.let { "https://youtube.com/playlist?list=${it.id}" }

    fun getDistance(other: IAlbum): Int {
        val levenshtein = LevenshteinDistance()
        var distance = levenshtein.apply(title.lowercase(), other.title.lowercase())

        year?.also { thisYear ->
            other.year?.also {
                distance += (it - thisYear).absoluteValue
            }
        }
        return distance
    }

    fun toImportableUiState() = ImportableAlbumUiState(
        albumId = albumId,
        albumType = albumType,
        fullImageUrl = fullImageUrl,
        isImported = isSaved,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        playCount = null,
        spotifyWebUrl = spotifyWebUrl,
        thumbnailUrl = thumbnailUrl,
        title = title,
        trackCount = trackCount,
        yearString = year?.toString(),
        youtubeWebUrl = youtubeWebUrl,
    )

    fun toUiState(isSelected: Boolean = false) = AlbumUiState(
        albumId = albumId,
        albumType = albumType,
        fullImageUrl = fullImageUrl,
        isInLibrary = isInLibrary && !isHidden,
        isLocal = isLocal,
        isSelected = isSelected,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        spotifyWebUrl = spotifyWebUrl,
        thumbnailUrl = thumbnailUrl,
        title = title,
        trackCount = trackCount,
        yearString = year?.toString(),
        youtubePlaylistId = youtubePlaylist?.id,
        youtubeWebUrl = youtubeWebUrl,
    )

    fun asSavedAlbum(): Album = Album(
        albumArt = albumArt,
        albumId = albumId,
        albumType = albumType,
        isHidden = isHidden,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        title = title,
        trackCount = trackCount,
        year = year,
        youtubePlaylist = youtubePlaylist,
    )

    fun asUnsavedAlbum(): UnsavedAlbum = UnsavedAlbum(
        albumId = albumId,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        title = title,
        albumArt = albumArt,
        albumType = albumType,
        isHidden = isHidden,
        musicBrainzReleaseGroupId = musicBrainzReleaseGroupId,
        musicBrainzReleaseId = musicBrainzReleaseId,
        spotifyId = spotifyId,
        trackCount = trackCount,
        year = year,
        youtubePlaylist = youtubePlaylist,
    )
}
