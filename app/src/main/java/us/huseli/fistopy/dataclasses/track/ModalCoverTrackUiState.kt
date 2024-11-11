package us.huseli.fistopy.dataclasses.track

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ModalCoverTrackUiState(
    override val albumId: String?,
    override val albumTitle: String?,
    override val artistString: String?,
    override val artists: ImmutableCollection<Artist>,
    override val fullImageUrl: String?,
    override val id: String,
    override val isDownloadable: Boolean,
    override val isInLibrary: Boolean,
    override val isPlayable: Boolean,
    override val musicBrainzReleaseGroupId: String?,
    override val musicBrainzReleaseId: String?,
    override val spotifyId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUrl: String?,
    override val title: String,
    override val youtubeWebUrl: String?,
    val durationMs: Long,
) : AbstractTrackUiState() {
    override val trackId: String
        get() = id

    companion object {
        fun fromTrackCombo(combo: ITrackCombo<*>) = ModalCoverTrackUiState(
            albumId = combo.track.albumId,
            albumTitle = combo.album?.title,
            artistString = combo.artistString,
            artists = combo.artists
                .map { Artist.fromArtistCredit(it) }
                .toImmutableList(),
            fullImageUrl = combo.fullImageUrl,
            id = combo.track.trackId,
            isDownloadable = combo.track.isDownloadable,
            isInLibrary = combo.track.isInLibrary,
            isPlayable = combo.track.isPlayable,
            musicBrainzReleaseGroupId = combo.album?.musicBrainzReleaseGroupId,
            musicBrainzReleaseId = combo.album?.musicBrainzReleaseId,
            spotifyId = combo.track.spotifyId,
            spotifyWebUrl = combo.track.spotifyWebUrl,
            thumbnailUrl = combo.fullImageUrl, // we want the same file regardless of expand/collapse status
            title = combo.track.title,
            youtubeWebUrl = combo.track.youtubeWebUrl,
            durationMs = combo.track.durationMs?.takeIf { it >= 0 } ?: 0,
        )
    }
}
