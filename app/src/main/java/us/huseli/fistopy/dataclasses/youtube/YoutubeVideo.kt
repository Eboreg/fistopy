package us.huseli.fistopy.dataclasses.youtube

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import kotlinx.parcelize.Parcelize
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.interfaces.IStringIdItem
import us.huseli.fistopy.stripCommonFixes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
@Immutable
data class YoutubeVideo(
    override val id: String,
    val title: String,
    val durationMs: Long? = null,
    val artist: String? = null,
    @Embedded("metadata_") val metadata: YoutubeMetadata? = null,
    @Embedded("thumbnail_") val thumbnail: YoutubeImage? = null,
    @Embedded("fullImage_") val fullImage: YoutubeImage? = null,
) : Parcelable, IExternalTrackComboProducer<YoutubeVideo>, IStringIdItem {
    val duration: Duration?
        get() = metadata?.durationMs?.milliseconds ?: durationMs?.milliseconds

    val metadataRefreshNeeded: Boolean
        get() = metadata == null || metadata.urlIsOld || metadata.lofiUrlIsOld

    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: Iterable<IAlbumArtistCredit>?,
        albumPosition: Int?
    ): ExternalTrackCombo<YoutubeVideo> {
        val track = Track(
            title = albumArtists?.toList()?.joined()
                ?.let { title.replace(Regex("^$it (- )?", RegexOption.IGNORE_CASE), "") }
                ?: title,
            isInLibrary = isInLibrary,
            albumId = album?.albumId,
            albumPosition = albumPosition,
            youtubeVideo = this,
            durationMs = metadata?.durationMs ?: durationMs,
            image = fullImage?.let {
                MediaStoreImage(
                    fullUriString = it.url,
                    thumbnailUriString = thumbnail?.url ?: it.url,
                )
            },
        )

        return ExternalTrackCombo(
            externalData = this,
            album = album,
            track = track,
            trackArtists = artist?.let { listOf(UnsavedTrackArtistCredit(name = it, trackId = track.trackId)) }
                ?: emptyList(),
            albumArtists = albumArtists?.toList() ?: emptyList(),
        )
    }
}

fun Iterable<YoutubeVideo>.stripTitleCommons(): List<YoutubeVideo> = zip(map { it.title }.stripCommonFixes())
    .map { (video, title) -> video.copy(title = title.replace(Regex(" \\([^)]*$"), "")) }
