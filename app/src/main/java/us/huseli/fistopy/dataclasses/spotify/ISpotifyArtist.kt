package us.huseli.fistopy.dataclasses.spotify

import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit

interface ISpotifyArtist {
    val id: String
    val name: String

    fun toNativeArtist(position: Int) = UnsavedArtistCredit(name = name, spotifyId = id, position = position)
}

fun Iterable<ISpotifyArtist>.artistString() = joinToString("/") { it.name }

fun Iterable<ISpotifyArtist>.toNativeAlbumArtists(albumId: String): List<UnsavedAlbumArtistCredit> =
    UnsavedAlbumArtistCredit.fromArtistCredits(toNativeArtists(), albumId = albumId)

fun Iterable<ISpotifyArtist>.toNativeArtists() =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artist -> artist.toNativeArtist(position = index) }

fun Iterable<ISpotifyArtist>.toNativeTrackArtists(trackId: String) =
    UnsavedTrackArtistCredit.fromArtistCredits(toNativeArtists(), trackId = trackId)
