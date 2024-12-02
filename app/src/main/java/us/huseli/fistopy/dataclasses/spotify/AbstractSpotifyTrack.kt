package us.huseli.fistopy.dataclasses.spotify

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.IArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.dataclasses.track.Track
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractSpotifyTrack<AT : ISpotifyArtist, T : AbstractSpotifyTrack<AT, T>> : AbstractSpotifyItem(),
    IExternalTrackComboProducer<T> {
    abstract val discNumber: Int
    abstract val durationMs: Int
    abstract override val id: String
    abstract val name: String
    abstract val trackNumber: Int
    abstract val artists: List<AT>

    val artist: String
        get() = artists.artistString()

    val duration: Duration
        get() = durationMs.milliseconds

    val title: String
        get() = name

    val trackArtists: Collection<IArtistCredit>
        get() = artists.toNativeArtists()

    @Suppress("UNCHECKED_CAST")
    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: Iterable<IAlbumArtistCredit>?,
        albumPosition: Int?,
    ): ExternalTrackCombo<T> {
        val track = toTrack(isInLibrary = isInLibrary, albumId = album?.albumId)

        return ExternalTrackCombo(
            externalData = this as T,
            album = album,
            track = track,
            trackArtists = artists.toNativeTrackArtists(track.trackId),
            albumArtists = albumArtists?.toList() ?: emptyList(),
        )
    }

    open fun toTrack(isInLibrary: Boolean, albumId: String? = null) = Track(
        albumPosition = trackNumber,
        discNumber = discNumber,
        spotifyId = id,
        title = name,
        durationMs = durationMs.toLong(),
        isInLibrary = isInLibrary,
        albumId = albumId,
    )
}