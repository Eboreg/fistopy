package us.huseli.fistopy.dataclasses.album

import kotlinx.collections.immutable.ImmutableList
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.ISelectableItem
import us.huseli.fistopy.interfaces.IStringIdItem

interface IAlbumUiState : IStringIdItem, IAlbumArtOwner, IHasMusicBrainzIds, ISelectableItem {
    override val fullImageUrl: String?
    override val isSelected: Boolean
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
    override val isPlayable: Boolean
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
}
