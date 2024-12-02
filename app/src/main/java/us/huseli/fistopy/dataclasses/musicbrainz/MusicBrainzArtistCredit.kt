package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.dataclasses.artist.UnsavedArtistCredit
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

fun Iterable<MusicBrainzArtistCredit>.toNativeArtists() =
    filter { it.name.lowercase() != "various artists" }
        .mapIndexed { index, artistCredit ->
            UnsavedArtistCredit(
                name = artistCredit.name,
                musicBrainzId = artistCredit.artist.id,
                position = index,
                joinPhrase = artistCredit.joinphrase ?: "/",
            )
        }

fun Iterable<MusicBrainzArtistCredit>.toNativeTrackArtists(trackId: String) =
    UnsavedTrackArtistCredit.fromArtistCredits(toNativeArtists(), trackId)
