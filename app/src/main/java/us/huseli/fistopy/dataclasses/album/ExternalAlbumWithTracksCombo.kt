package us.huseli.fistopy.dataclasses.album

import kotlinx.collections.immutable.toImmutableList
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.interfaces.IStringIdItem

data class ExternalAlbumWithTracksCombo<T : IStringIdItem>(
    val externalData: T,
    val playCount: Int? = null,
    override val album: UnsavedAlbum,
    override val artists: List<IAlbumArtistCredit>,
    override val tags: List<Tag>,
    override val trackCombos: List<ExternalTrackCombo<*>>,
) : IAlbumWithTracksCombo<UnsavedAlbum, ExternalAlbumWithTracksCombo<T>>, IUnsavedAlbumCombo {
    override fun toImportableUiState(): ImportableAlbumUiState {
        return album.toImportableUiState().copy(
            artistString = artists.joined(),
            artists = artists.toImmutableList(),
            isDownloadable = isDownloadable,
            playCount = playCount,
        )
    }
}
