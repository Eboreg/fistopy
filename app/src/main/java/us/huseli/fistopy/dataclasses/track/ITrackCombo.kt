package us.huseli.fistopy.dataclasses.track

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.apache.commons.text.similarity.LevenshteinDistance
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.artist.AlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.IArtistCredit
import us.huseli.fistopy.dataclasses.artist.ITrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.TrackArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import kotlin.math.absoluteValue

interface ITrackCombo<T : ITrackCombo<T>> : Comparable<ITrackCombo<*>>, IAlbumArtOwner, IHasMusicBrainzIds {
    class TrackMatch<T>(
        val distance: Int,
        val trackCombo: T,
    )

    val track: Track
    val album: IAlbum?
    val trackArtists: List<ITrackArtistCredit>
    val albumArtists: List<IAlbumArtistCredit>

    val artists: List<IArtistCredit>
        get() = trackArtists.takeIf { it.isNotEmpty() } ?: albumArtists

    val artistString: String?
        get() = trackArtists.joined() ?: albumArtists.joined()

    val year: Int?
        get() = track.year ?: album?.year

    override val fullImageUrl: String?
        get() = album?.fullImageUrl ?: track.fullImageUrl

    override val thumbnailUrl: String?
        get() = album?.thumbnailUrl ?: track.thumbnailUrl

    override val musicBrainzReleaseGroupId: String?
        get() = album?.musicBrainzReleaseGroupId

    override val musicBrainzReleaseId: String?
        get() = album?.musicBrainzReleaseId

    override fun compareTo(other: ITrackCombo<*>): Int {
        if (track.discNumberNonNull != other.track.discNumberNonNull)
            return track.discNumberNonNull - other.track.discNumberNonNull
        if (track.albumPositionNonNull != other.track.albumPositionNonNull)
            return track.albumPositionNonNull - other.track.albumPositionNonNull
        return track.title.compareTo(other.track.title)
    }

    fun getDistance(other: ITrackCombo<*>): Int {
        val levenshtein = LevenshteinDistance()
        val title = track.title.lowercase()
        val titleDistances = mutableListOf<Int>()
        val combinedTitle = trackArtists.joined()?.let { "$it - $title" }?.lowercase() ?: title
        var distance = 0

        // Test various permutations of "[artist] - [title]":
        if (this.trackArtists.isEmpty()) {
            titleDistances.add(levenshtein.apply(other.track.title.lowercase(), title))
        }
        trackArtists.joined()?.also {
            titleDistances.add(levenshtein.apply("$it - ${other.track.title}".lowercase(), combinedTitle))
        }
        other.albumArtists.joined()?.also {
            titleDistances.add(levenshtein.apply("$it - ${other.track.title}".lowercase(), combinedTitle))
        }
        for (otherArtist in other.albumArtists + other.trackArtists) {
            if (trackArtists.isEmpty()) {
                titleDistances.add(
                    levenshtein.apply(
                        "${otherArtist.name} - ${other.track.title}".lowercase(),
                        title,
                    )
                )
            } else {
                for (artist in trackArtists) {
                    titleDistances.add(
                        levenshtein.apply(
                            "${otherArtist.name} - ${other.track.title}".lowercase(),
                            "${artist.name} - $title".lowercase(),
                        )
                    )
                }
            }
        }
        distance += titleDistances.min()

        // Add number of seconds diffing:
        track.duration?.inWholeSeconds
            ?.let { other.track.duration?.inWholeSeconds?.minus(it) }
            ?.also { distance += it.toInt().absoluteValue }

        return distance
    }

    @Suppress("UNCHECKED_CAST")
    fun matchTrack(other: ITrackCombo<*>) =
        TrackMatch<T>(distance = getDistance(other), trackCombo = this as T)

    fun toUiState(isSelected: Boolean = false): TrackUiState = track.toUiState(isSelected = isSelected).copy(
        albumTitle = album?.title,
        artists = artists
            .map { AbstractTrackUiState.Artist.fromArtistCredit(it) }
            .toImmutableList(),
        artistString = artistString,
        fullImageUrl = album?.fullImageUrl ?: track.fullImageUrl,
        musicBrainzReleaseGroupId = album?.musicBrainzReleaseGroupId,
        musicBrainzReleaseId = album?.musicBrainzReleaseId,
        thumbnailUrl = album?.thumbnailUrl ?: track.thumbnailUrl,
    )

    fun withTrack(track: Track): T

    private fun getPositionString(albumDiscCount: Int): String =
        if (albumDiscCount > 1 && track.discNumber != null && track.albumPosition != null)
            "${track.discNumber}.${track.albumPosition}"
        else track.albumPosition?.toString() ?: ""
}


fun Iterable<ITrackCombo<*>>.tracks(): List<Track> = map { it.track }

fun Iterable<ITrackCombo<*>>.toUiStates(): ImmutableList<TrackUiState> = map { it.toUiState() }.toImmutableList()

fun Iterable<ITrackCombo<*>>.asUnsavedTrackCombos() = map { trackCombo ->
    UnsavedTrackCombo(
        track = trackCombo.track,
        album = trackCombo.album?.asUnsavedAlbum(),
        trackArtists = trackCombo.trackArtists,
        albumArtists = trackCombo.albumArtists,
    )
}

interface ISavedTrackCombo<T : ISavedTrackCombo<T>> : ITrackCombo<T> {
    override val trackArtists: List<TrackArtistCredit>
    override val albumArtists: List<AlbumArtistCredit>
    override val album: Album?
}
