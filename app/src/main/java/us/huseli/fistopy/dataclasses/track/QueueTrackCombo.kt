package us.huseli.fistopy.dataclasses.track

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.TrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.interfaces.ILogger
import us.huseli.fistopy.umlautify
import java.util.UUID

@DatabaseView(
    """
    SELECT
        TrackCombo.*,
        COALESCE(Track_localUri, Track_youtubeVideo_metadata_url) AS QueueTrackCombo_uri,
        QueueTrack_queueTrackId,
        QueueTrack_position
    FROM QueueTrack JOIN TrackCombo ON Track_trackId = QueueTrack_trackId
    GROUP BY QueueTrack_queueTrackId
    HAVING QueueTrackCombo_uri IS NOT NULL
    ORDER BY QueueTrack_position, QueueTrack_queueTrackId
    """
)
@Immutable
data class QueueTrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album?,
    @ColumnInfo("QueueTrackCombo_uri") val uri: String,
    @ColumnInfo("QueueTrack_queueTrackId") val queueTrackId: String = UUID.randomUUID().toString(),
    @ColumnInfo("QueueTrack_position") val position: Int = 0,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId", entity = TrackArtistCredit::class)
    override val trackArtists: List<TrackArtistCredit>,
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId", entity = AlbumArtistCredit::class)
    override val albumArtists: List<AlbumArtistCredit> = emptyList(),
) : ISavedTrackCombo<QueueTrackCombo>, ILogger {
    val metadataRefreshNeeded: Boolean
        get() = track.youtubeVideo?.metadataRefreshNeeded == true

    val queueTrack: QueueTrack
        get() = QueueTrack(queueTrackId = queueTrackId, trackId = track.trackId, position = position)

    @Ignore
    val mediaItem = MediaItem.Builder()
        .setMediaId(queueTrackId)
        .setUri(uri)
        .setMediaMetadata(getMediaMetadata())
        .setTag(this)
        .build()

    fun hasSameTrack(other: QueueTrackCombo?) = other != null &&
        other.track.trackId == track.trackId &&
        other.queueTrackId == queueTrackId &&
        other.uri == uri &&
        other.position == position

    override fun toUiState(isSelected: Boolean): TrackUiState =
        super.toUiState(isSelected = isSelected).copy(id = queueTrackId)

    override fun withTrack(track: Track) = copy(track = track)

    private fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata.Builder()
            .setArtist(artistString?.umlautify())
            .setTitle(track.title.umlautify())
            .setAlbumArtist(albumArtists.joined()?.umlautify())
            .setAlbumTitle(album?.title?.umlautify())
            .setDiscNumber(track.discNumber)
            .setReleaseYear(track.year ?: album?.year)
            .setArtworkUri(fullImageUrl?.toUri() ?: thumbnailUrl?.toUri())
            .build()
    }
}

fun Iterable<QueueTrackCombo>.reindexed(offset: Int = 0): List<QueueTrackCombo> =
    mapIndexed { index, combo -> combo.copy(position = index + offset) }

fun Iterable<QueueTrackCombo>.plus(item: QueueTrackCombo, index: Int): List<QueueTrackCombo> =
    toMutableList().apply { add(index, item) }.toList()

fun Iterable<QueueTrackCombo>.containsWithPosition(other: QueueTrackCombo): Boolean =
    any { it == other && it.position == other.position }
