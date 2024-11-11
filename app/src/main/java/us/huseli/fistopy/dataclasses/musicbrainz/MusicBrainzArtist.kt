package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit

data class MusicBrainzArtistCredit(
    val artist: MusicBrainzArtist,
    val joinphrase: String?,
    val name: String,
) {
    data class MusicBrainzArtist(
        override val id: String,
        val name: String,
    ) : AbstractMusicBrainzItem()
}

fun List<MusicBrainzArtistCredit>.joined(): String = mapIndexed { index, artistCredit ->
    artistCredit.name + if (index < lastIndex) artistCredit.joinphrase ?: "/" else ""
}.joinToString("")

fun Iterable<MusicBrainzArtistCredit>.toNativeAlbumArtists(albumId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artistCredit ->
            UnsavedAlbumArtistCredit(
                name = artistCredit.name,
                musicBrainzId = artistCredit.artist.id,
                albumId = albumId,
                position = index,
                joinPhrase = artistCredit.joinphrase ?: "/",
            )
        }

fun Iterable<MusicBrainzArtistCredit>.toNativeTrackArtists(trackId: String) =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artistCredit ->
            UnsavedTrackArtistCredit(
                name = artistCredit.name,
                musicBrainzId = artistCredit.artist.id,
                trackId = trackId,
                position = index,
                joinPhrase = artistCredit.joinphrase ?: "/",
            )
        }
