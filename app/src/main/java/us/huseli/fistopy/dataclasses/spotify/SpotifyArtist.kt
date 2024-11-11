package us.huseli.fistopy.dataclasses.spotify

import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedArtist
import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit

interface ISpotifyArtist {
    val id: String
    val name: String
}

data class SpotifySimplifiedArtist(
    override val id: String,
    override val name: String,
) : AbstractSpotifyItem(), ISpotifyArtist

data class SpotifyArtist(
    override val id: String,
    override val name: String,
    val images: List<SpotifyImage>,
    val popularity: Int?,
) : AbstractSpotifyItem(), ISpotifyArtist {
    fun toNativeArtist() = UnsavedArtist(
        name = name,
        spotifyId = id,
        image = images.toMediaStoreImage(),
    )
}

fun Iterable<SpotifyArtist>.toNativeArtists(): List<UnsavedArtist> = map { it.toNativeArtist() }

fun Iterable<ISpotifyArtist>.artistString() = joinToString("/") { it.name }

fun Iterable<ISpotifyArtist>.toNativeAlbumArtists(albumId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist ->
            UnsavedAlbumArtistCredit(
                name = artist.name,
                spotifyId = artist.id,
                albumId = albumId,
                position = index,
            )
        }

fun Iterable<ISpotifyArtist>.toNativeArtistCredits() =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist ->
            UnsavedArtistCredit(
                name = artist.name,
                spotifyId = artist.id,
                position = index,
            )
        }

fun Iterable<ISpotifyArtist>.toNativeTrackArtists(trackId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist ->
            UnsavedTrackArtistCredit(
                name = artist.name,
                spotifyId = artist.id,
                trackId = trackId,
                position = index,
            )
        }
