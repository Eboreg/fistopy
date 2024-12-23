package us.huseli.fistopy.dataclasses.track

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableCollection

@Immutable
data class AlbumTrackUiState(
    override val albumId: String?,
    override val albumTitle: String?,
    override val artistString: String?,
    override val artists: ImmutableCollection<Artist>,
    override val fullImageUrl: String?,
    override val id: String,
    override val isDownloadable: Boolean,
    override val isInLibrary: Boolean,
    override val isPlayable: Boolean,
    override val isSelected: Boolean,
    override val musicBrainzReleaseGroupId: String?,
    override val musicBrainzReleaseId: String?,
    override val spotifyId: String?,
    override val spotifyWebUrl: String?,
    override val thumbnailUrl: String?,
    override val title: String,
    override val youtubeWebUrl: String?,
    val durationMs: Long?,
    val positionString: String,
) : AbstractTrackUiState(), ISelectableTrackUiState {
    override val trackId: String
        get() = id

    override fun withIsSelected(value: Boolean) = copy(isSelected = value)
}
