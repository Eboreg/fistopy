package us.huseli.fistopy.dataclasses.spotify

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.musicbrainz.capitalizeGenreName
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo


data class SpotifySimplifiedAlbum(
    @SerializedName("album_type")
    override val spotifyAlbumType: SpotifyAlbumType?,
    override val artists: List<SpotifySimplifiedArtist>,
    override val id: String,
    override val images: List<SpotifyImage>,
    override val name: String,
    @SerializedName("release_date")
    override val releaseDate: String,
    @SerializedName("total_tracks")
    override val totalTracks: Int,
) : AbstractSpotifyAlbum<SpotifySimplifiedAlbum>()


data class SpotifyAlbum(
    @SerializedName("album_type")
    override val spotifyAlbumType: SpotifyAlbumType?,
    override val artists: List<SpotifySimplifiedArtist>,
    override val id: String,
    override val images: List<SpotifyImage>,
    override val name: String,
    @SerializedName("release_date")
    override val releaseDate: String,
    @SerializedName("total_tracks")
    override val totalTracks: Int,
    val genres: List<String>,
    val tracks: SpotifyResponse<SpotifySimplifiedTrack>,
) : AbstractSpotifyAlbum<SpotifyAlbum>() {
    override fun getTrackCombos(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: List<IAlbumArtistCredit>?,
    ): List<ExternalTrackCombo<*>> = tracks.items.map { track ->
        track.toTrackCombo(album = album, albumArtists = albumArtists, isInLibrary = isInLibrary)
    }

    override fun getTags(): List<Tag> = genres.map { Tag(name = capitalizeGenreName(it)) }
}


data class SpotifySavedAlbumObject(
    @SerializedName("added_at")
    val addedAt: String,
    val album: SpotifyAlbum,
)
