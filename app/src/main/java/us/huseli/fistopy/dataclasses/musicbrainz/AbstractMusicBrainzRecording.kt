package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.dataclasses.track.Track

abstract class AbstractMusicBrainzRecording<T : AbstractMusicBrainzRecording<T>> : AbstractMusicBrainzItem(),
    IExternalTrackComboProducer<T> {
    abstract override val id: String
    abstract val artistCredit: List<MusicBrainzArtistCredit>
    abstract val firstReleaseDate: String?
    abstract val length: Int
    abstract val title: String

    val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    @Suppress("UNCHECKED_CAST")
    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: Iterable<IAlbumArtistCredit>?,
        albumPosition: Int?,
    ): ExternalTrackCombo<T> {
        val track = Track(
            albumId = album?.albumId,
            albumPosition = albumPosition,
            durationMs = length.toLong(),
            musicBrainzId = id,
            title = title,
            year = year,
            isInLibrary = isInLibrary,
        )

        return ExternalTrackCombo(
            externalData = this as T,
            album = album,
            track = track,
            trackArtists = artistCredit.toNativeTrackArtists(track.trackId),
            albumArtists = albumArtists?.toList() ?: emptyList(),
        )
    }
}
