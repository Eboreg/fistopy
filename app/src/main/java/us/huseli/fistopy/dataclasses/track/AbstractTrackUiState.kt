package us.huseli.fistopy.dataclasses.track

import kotlinx.collections.immutable.ImmutableCollection
import us.huseli.fistopy.dataclasses.artist.IArtistCredit
import us.huseli.fistopy.dataclasses.artist.ISavedArtistCredit
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractTrackUiState : IStringIdItem, IAlbumArtOwner, IHasMusicBrainzIds {
    abstract val albumId: String?
    abstract val albumTitle: String?
    abstract val artistString: String?
    abstract val artists: ImmutableCollection<Artist>
    abstract override val fullImageUrl: String?
    abstract val isDownloadable: Boolean
    abstract val isInLibrary: Boolean
    abstract val isPlayable: Boolean
    abstract override val musicBrainzReleaseGroupId: String?
    abstract override val musicBrainzReleaseId: String?
    abstract val spotifyId: String?
    abstract val spotifyWebUrl: String?
    abstract override val thumbnailUrl: String?
    abstract val title: String
    abstract val trackId: String
    abstract val youtubeWebUrl: String?

    data class Artist(val name: String, val id: String?) {
        companion object {
            fun fromArtistCredit(artistCredit: IArtistCredit): Artist {
                return Artist(
                    name = artistCredit.name,
                    id = if (artistCredit is ISavedArtistCredit) artistCredit.artistId else null,
                )
            }
        }
    }
}
