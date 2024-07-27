package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.UnsavedTrackCombo
import us.huseli.fistopy.interfaces.IExternalTrack

data class MusicBrainzRecording(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    val disambiguation: String?,
    @SerializedName("first-release-date")
    val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val length: Int,
    override val title: String,
) : AbstractMusicBrainzItem(), IExternalTrack {
    val artist: String?
        get() = artistCredit.joined().takeIf { it.isNotEmpty() }

    val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    override fun toTrackCombo(isInLibrary: Boolean, album: IAlbum?): UnsavedTrackCombo {
        val track = Track(
            isInLibrary = isInLibrary,
            musicBrainzId = id,
            title = title,
            year = year,
            durationMs = length.toLong(),
            albumId = album?.albumId,
        )

        return UnsavedTrackCombo(
            track = track,
            trackArtists = artistCredit.toNativeTrackArtists(trackId = track.trackId),
            album = album,
        )
    }
}
