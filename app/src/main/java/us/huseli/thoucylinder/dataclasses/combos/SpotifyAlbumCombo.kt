package us.huseli.thoucylinder.dataclasses.combos

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbum
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyAlbumGenre
import us.huseli.thoucylinder.dataclasses.entities.SpotifyArtist
import us.huseli.thoucylinder.dataclasses.entities.SpotifyTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class SpotifyAlbumCombo(
    @Embedded val spotifyAlbum: SpotifyAlbum,
    @Relation(
        entity = SpotifyArtist::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "SpotifyArtist_id",
        associateBy = Junction(
            value = SpotifyAlbumArtist::class,
            parentColumn = "SpotifyAlbumArtist_albumId",
            entityColumn = "SpotifyAlbumArtist_artistId",
        )
    )
    val artists: List<SpotifyArtist>,
    @Relation(
        entity = Genre::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "Genre_genreName",
        associateBy = Junction(
            value = SpotifyAlbumGenre::class,
            parentColumn = "SpotifyAlbumGenre_albumId",
            entityColumn = "SpotifyAlbumGenre_genreName",
        )
    )
    val genres: List<Genre>,
    @Relation(
        entity = SpotifyTrack::class,
        parentColumn = "SpotifyAlbum_id",
        entityColumn = "SpotifyTrack_spotifyAlbumId",
    )
    val spotifyTrackCombos: List<SpotifyTrackCombo>,
) {
    val artist: String
        get() = artists.joinToString("/") { it.name }

    val durationMs: Int
        get() = spotifyTrackCombos.sumOf { it.track.durationMs }

    val duration: Duration
        get() = durationMs.milliseconds

    private val primaryArtist: String?
        get() = artists.firstOrNull()?.name

    val searchQuery: String
        get() = primaryArtist?.let { "$it - ${spotifyAlbum.name}" } ?: spotifyAlbum.name
}


fun List<SpotifyAlbumCombo>.filterBySearchTerm(term: String): List<SpotifyAlbumCombo> {
    val words = term.lowercase().split(Regex(" +"))

    return filter { combo ->
        words.all {
            combo.artist.lowercase().contains(it) ||
                combo.spotifyAlbum.name.lowercase().contains(it) ||
                combo.spotifyAlbum.year?.toString()?.contains(it) == true
        }
    }
}