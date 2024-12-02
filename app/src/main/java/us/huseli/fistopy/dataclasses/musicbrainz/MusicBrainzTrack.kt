package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.dataclasses.track.Track

data class MusicBrainzTrack(
    @SerializedName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>,
    override val id: String,
    val length: Int,
    val number: String,
    val position: Int,
    val recording: MusicBrainzRecording,
    val title: String,
) : AbstractMusicBrainzItem(), IExternalTrackComboProducer<MusicBrainzTrack> {
    val year: Int? = recording.year

    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: Iterable<IAlbumArtistCredit>?,
        albumPosition: Int?,
    ): ExternalTrackCombo<MusicBrainzTrack> {
        return this.toTrackCombo(
            isInLibrary = isInLibrary,
            album = album,
            albumArtists = albumArtists ?: emptyList(),
            discNumber = null,
            albumPosition = albumPosition,
        )
    }

    fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum? = null,
        albumArtists: Iterable<IAlbumArtistCredit> = emptyList(),
        discNumber: Int? = null,
        albumPosition: Int? = null,
    ): ExternalTrackCombo<MusicBrainzTrack> {
        val track = Track(
            musicBrainzId = recording.id,
            isInLibrary = isInLibrary,
            year = year,
            title = title,
            durationMs = length.toLong(),
            albumPosition = number.toIntOrNull() ?: albumPosition ?: position,
            albumId = album?.albumId,
            discNumber = discNumber,
        )

        return ExternalTrackCombo(
            externalData = this,
            album = album,
            track = track,
            trackArtists = artistCredit.toNativeTrackArtists(trackId = track.trackId),
            albumArtists = albumArtists.toList(),
        )
    }
}
