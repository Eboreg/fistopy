package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.IArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.dataclasses.track.Track
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractSpotifyTrack<AT : ISpotifyArtist, T : AbstractSpotifyTrack<AT, T>> : AbstractSpotifyItem(), IExternalTrackComboProducer<T> {
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
        get() = artists.toNativeArtistCredits()

    @Suppress("UNCHECKED_CAST")
    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: List<IAlbumArtistCredit>?,
        albumPosition: Int?
    ): ExternalTrackCombo<T> {
        val track = toTrack(isInLibrary = isInLibrary, albumId = album?.albumId)

        return ExternalTrackCombo(
            externalData = this as T,
            album = album,
            track = track,
            trackArtists = artists.toNativeTrackArtists(track.trackId),
            albumArtists = albumArtists ?: emptyList(),
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


data class SpotifySimplifiedTrack(
    override val artists: List<SpotifySimplifiedArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
) : AbstractSpotifyTrack<SpotifySimplifiedArtist, SpotifySimplifiedTrack>()


data class SpotifyTrack(
    override val artists: List<SpotifyArtist>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val id: String,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    val album: SpotifySimplifiedAlbum,
    val popularity: Int,
) : AbstractSpotifyTrack<SpotifyArtist, SpotifyTrack>() {
    override fun toTrack(isInLibrary: Boolean, albumId: String?): Track =
        super.toTrack(isInLibrary = isInLibrary, albumId = albumId).copy(image = album.images.toMediaStoreImage())

    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: List<IAlbumArtistCredit>?,
        albumPosition: Int?
    ): ExternalTrackCombo<SpotifyTrack> {
        val finalAlbum = album ?: this.album.toAlbum(isLocal = false, isInLibrary = isInLibrary)

        return super.toTrackCombo(
            isInLibrary = isInLibrary,
            album = finalAlbum,
            albumPosition = albumPosition,
            albumArtists = albumArtists ?: this.album.artists.toNativeAlbumArtists(albumId = finalAlbum.albumId),
        )
    }
}


data class SpotifyTrackIdPair(val spotifyTrackId: String, val trackId: String)
