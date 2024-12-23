package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.retaintheme.extensions.capitalized

data class MusicBrainzGenre(
    val count: Int,
    override val id: String,
    val name: String,
) : AbstractMusicBrainzItem() {
    override fun equals(other: Any?) = other is MusicBrainzGenre && other.id == id
    override fun hashCode(): Int = 31 * super.hashCode() + id.hashCode()
}

fun Iterable<MusicBrainzGenre>.toInternal(): List<Tag> =
    map { Tag(name = capitalizeGenreName(it.name), isMusicBrainzGenre = true) }

fun capitalizeGenreName(name: String): String {
    val specialCases = listOf(
        "AOR",
        "ASMR",
        "EAI",
        "EBM",
        "EDM",
        "FM Synthesis",
        "Hi-NRG",
        "IDM",
        "MPB",
        "OPM",
        "RKT",
        "Trap EDM",
        "UK Drill",
        "UK Funky",
        "UK Garage",
        "UK Hardcore",
        "UK Jackin",
        "UK Street Soul",
        "UK82",
    ).associateBy { it.lowercase() }

    return specialCases[name.lowercase()] ?: name.capitalized()
}
