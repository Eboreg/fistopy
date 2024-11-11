package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.Track

data class MusicBrainzMedia(
    val format: String?,
    @SerializedName("track-count")
    val trackCount: Int,
    val position: Int,
    @SerializedName("track-offset")
    val trackOffset: Int,
    val tracks: List<MusicBrainzTrack>,
)

fun Iterable<MusicBrainzMedia>.toTrackCombos(
    isInLibrary: Boolean,
    album: UnsavedAlbum? = null,
    albumArtists: List<IAlbumArtistCredit>? = null,
): List<ExternalTrackCombo<MusicBrainzTrack>> = flatMap { medium ->
    medium.tracks.map { mbTrack ->
        val track = Track(
            title = mbTrack.title,
            albumPosition = mbTrack.position,
            discNumber = medium.position,
            year = mbTrack.year,
            musicBrainzId = mbTrack.recording.id,
            albumId = album?.albumId,
            isInLibrary = isInLibrary,
        )
        val trackArtists = mbTrack.artistCredit.toNativeTrackArtists(track.trackId)

        ExternalTrackCombo(
            externalData = mbTrack,
            album = album,
            trackArtists = trackArtists,
            albumArtists = albumArtists ?: emptyList(),
            track = track,
        )
    }
}
