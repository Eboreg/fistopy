package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo

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
    albumArtists: List<IAlbumArtistCredit> = emptyList(),
): List<ExternalTrackCombo<MusicBrainzTrack>> = flatMap { medium ->
    medium.tracks.map { mbTrack ->
        mbTrack.toTrackCombo(
            isInLibrary = isInLibrary,
            album = album,
            albumArtists = albumArtists,
            discNumber = medium.position,
            albumPosition = mbTrack.position,
        )
    }
}
