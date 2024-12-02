package us.huseli.fistopy.dataclasses.album

import kotlinx.collections.immutable.toImmutableList
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.interfaces.IStringIdItem

interface IAlbumCombo<A : IAlbum> : IStringIdItem {
    val album: A
    val artists: List<IAlbumArtistCredit>
    val minYear: Int?
    val maxYear: Int?
    val isPartiallyDownloaded: Boolean
    val unplayableTrackCount: Int
    val isDownloadable: Boolean

    val artistNames: List<String>
        get() = artists.map { it.name }

    private val years: Pair<Int, Int>?
        get() {
            val year = this.album.year?.takeIf { it > 1000 }
            val minYear = this.minYear?.takeIf { it > 1000 }
            val maxYear = this.maxYear?.takeIf { it > 1000 }

            return if (year != null) Pair(year, year)
            else if (minYear != null && maxYear != null) Pair(minYear, maxYear)
            else null
        }

    val yearString: String?
        get() = years?.let { (min, max) ->
            if (min == max) min.toString()
            else "$min–$max"
        }

    override val id: String
        get() = album.albumId

    fun getDistance(other: IAlbumCombo<*>): Double {
        val levenshtein = LevenshteinDistance()
        val thisAlbumArtistString = artists.joined()?.lowercase()
        val otherAlbumArtistString = other.artists.joined()?.lowercase()
        val otherAlbumArtistNames = listOfNotNull(otherAlbumArtistString).plus(other.artistNames)
        // Strip "[artist] - " from our album title, by other's artists:
        val thisAlbum = album.let { album ->
            var title = album.title

            for (artistName in otherAlbumArtistNames) {
                title = title.replace(Regex("^$artistName( - *)?", RegexOption.IGNORE_CASE), "")
            }
            album.asUnsavedAlbum().copy(title = title)
        }
        val albumDistance = thisAlbum.getDistance(other.album)
        val artistDistance =
            if (thisAlbumArtistString != null && otherAlbumArtistString != null)
                levenshtein.apply(thisAlbumArtistString, otherAlbumArtistString)
            else 0

        return (albumDistance + artistDistance) / 2.0
    }

    fun toImportableUiState() =
        album.toImportableUiState().copy(
            artistString = artists.joined(),
            artists = artists.toImmutableList(),
            isDownloadable = isDownloadable,
        )

    fun toUiState(isSelected: Boolean = false) = album.toUiState().copy(
        artistString = artists.joined(),
        artists = artists.toImmutableList(),
        isDownloadable = isDownloadable,
        isPartiallyDownloaded = isPartiallyDownloaded,
        isSelected = isSelected,
        unplayableTrackCount = unplayableTrackCount,
        yearString = yearString,
    )
}

fun Iterable<IAlbumCombo<*>>.toUiStates() = map { it.toUiState() }.toImmutableList()
