package us.huseli.fistopy.dataclasses.album

import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.IStringIdItem

interface IAlbumUiState : IStringIdItem, IAlbumArtOwner, IHasMusicBrainzIds {
    override val fullImageUrl: String?
    override val musicBrainzReleaseGroupId: String?
    override val musicBrainzReleaseId: String?
    override val thumbnailUrl: String?
    val albumId: String
    val albumType: AlbumType?
    val artistString: String?
    val artists: ImmutableList<IAlbumArtistCredit>
    val isDownloadable: Boolean
    val isInLibrary: Boolean
    val isLocal: Boolean
    val isOnYoutube: Boolean
    val isPlayable: Boolean
    val isSaved: Boolean
    val isSelected: Boolean
    val spotifyWebUrl: String?
    val title: String
    val trackCount: Int?
    val yearString: String?
    val youtubeWebUrl: String?

    override val id: String
        get() = albumId

    fun matchesSearchTerm(term: String): Boolean {
        val words = term.lowercase().split(Regex(" +"))

        return words.all {
            artistString?.lowercase()?.contains(it) == true ||
                title.lowercase().contains(it) ||
                yearString?.contains(it) == true
        }
    }

    fun withIsSelected(value: Boolean): IAlbumUiState
}
