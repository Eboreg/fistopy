package us.huseli.fistopy.dataclasses.spotify

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.R
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbumProducer
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.enums.AlbumType

enum class SpotifyAlbumType(@StringRes val stringRes: Int) {
    @SerializedName("album")
    ALBUM(R.string.album),

    @SerializedName("single")
    SINGLE(R.string.single),

    @SerializedName("compilation")
    COMPILATION(R.string.compilation);

    val nativeAlbumType: AlbumType
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            COMPILATION -> AlbumType.COMPILATION
        }
}

abstract class AbstractSpotifyAlbum<T : AbstractSpotifyAlbum<T>> : AbstractSpotifyItem(),
    IAlbumProducer, IExternalAlbumWithTracksProducer<T> {
    abstract val spotifyAlbumType: SpotifyAlbumType?
    abstract val artists: List<SpotifySimplifiedArtist>
    abstract override val id: String
    abstract val images: List<SpotifyImage>
    abstract val name: String
    abstract val releaseDate: String
    abstract val totalTracks: Int

    open fun getTrackCombos(
        isInLibrary: Boolean,
        album: UnsavedAlbum? = null,
        albumArtists: List<IAlbumArtistCredit>? = null,
    ): List<ExternalTrackCombo<*>> = emptyList()

    open fun getTags(): List<Tag> = emptyList()

    override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String) = UnsavedAlbum(
        albumArt = images.toMediaStoreImage(),
        albumId = albumId,
        albumType = spotifyAlbumType?.nativeAlbumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        spotifyId = id,
        title = name,
        trackCount = totalTracks,
        year = releaseDate.substringBefore('-').toIntOrNull(),
    )

    @Suppress("UNCHECKED_CAST")
    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String
    ): ExternalAlbumWithTracksCombo<T> {
        val album = toAlbum(
            isLocal = isLocal,
            isInLibrary = isInLibrary,
            albumId = albumId,
        )
        val albumArtists = artists.toNativeAlbumArtists(album.albumId)

        return ExternalAlbumWithTracksCombo(
            album = album,
            artists = albumArtists,
            tags = getTags(),
            trackCombos = getTrackCombos(
                isInLibrary = isInLibrary,
                album = album,
                albumArtists = albumArtists,
            ),
            externalData = this as T,
        )
    }

    override fun toString(): String = artists.artistString().takeIf { it.isNotEmpty() }?.let { "$it - $name" } ?: name
}
