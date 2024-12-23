package us.huseli.fistopy.dataclasses.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import us.huseli.fistopy.dataclasses.MediaStoreImage
import java.util.UUID

@DatabaseView(
    """
    SELECT TrackArtist.*,
        Artist_name AS TrackArtist_name,
        Artist_spotifyId AS TrackArtist_spotifyId,
        Artist_musicBrainzId AS TrackArtist_musicBrainzId,
        Artist_image_fullUriString AS TrackArtist_image_fullUriString,
        Artist_image_thumbnailUriString AS TrackArtist_image_thumbnailUriString
    FROM TrackArtist JOIN Artist ON TrackArtist_artistId = Artist_id
    ORDER BY TrackArtist_position
    """
)
@Immutable
data class TrackArtistCredit(
    @ColumnInfo("TrackArtist_trackId") override val trackId: String,
    @ColumnInfo("TrackArtist_artistId") override val artistId: String = UUID.randomUUID().toString(),
    @ColumnInfo("TrackArtist_name") override val name: String,
    @ColumnInfo("TrackArtist_spotifyId") override val spotifyId: String? = null,
    @ColumnInfo("TrackArtist_musicBrainzId") override val musicBrainzId: String? = null,
    @ColumnInfo("TrackArtist_joinPhrase") override val joinPhrase: String = "/",
    @Embedded("TrackArtist_image_") override val image: MediaStoreImage? = null,
    @ColumnInfo("TrackArtist_position") override val position: Int = 0,
) : ITrackArtistCredit, ISavedArtist {
    override fun withTrackId(trackId: String): TrackArtistCredit = copy(trackId = trackId)
}

fun Iterable<TrackArtistCredit>.toTrackArtists(): List<TrackArtist> = map {
    TrackArtist(
        trackId = it.trackId,
        artistId = it.artistId,
        joinPhrase = it.joinPhrase,
        position = it.position,
    )
}