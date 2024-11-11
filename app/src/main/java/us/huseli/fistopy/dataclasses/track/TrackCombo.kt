package us.huseli.fistopy.dataclasses.track

import androidx.compose.runtime.Immutable
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.TrackArtistCredit

@DatabaseView(
    """
    SELECT Track.*, Album.*
    FROM Track LEFT JOIN Album ON Track_albumId = Album_albumId
    GROUP BY Track_trackId
    ORDER BY Track_discNumber, Track_albumPosition, Track_title
    """
)
@Immutable
data class TrackCombo(
    @Embedded override val track: Track,
    @Embedded override val album: Album? = null,
    @Relation(parentColumn = "Track_trackId", entityColumn = "TrackArtist_trackId", entity = TrackArtistCredit::class)
    override val trackArtists: List<TrackArtistCredit> = emptyList<TrackArtistCredit>(),
    @Relation(parentColumn = "Track_albumId", entityColumn = "AlbumArtist_albumId", entity = AlbumArtistCredit::class)
    override val albumArtists: List<AlbumArtistCredit> = emptyList<AlbumArtistCredit>(),
) : ISavedTrackCombo<TrackCombo> {
    override fun equals(other: Any?) = other is TrackCombo
        && other.track == track
        && other.trackArtists == trackArtists
        && other.albumArtists == albumArtists

    override fun hashCode(): Int = 31 * (31 * track.hashCode() + trackArtists.hashCode()) + albumArtists.hashCode()

    override fun withTrack(track: Track) = copy(track = track)
}
