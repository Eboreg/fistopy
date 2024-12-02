package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.Track


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
        albumArtists: Iterable<IAlbumArtistCredit>?,
        albumPosition: Int?,
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
