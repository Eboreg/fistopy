package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ITrackCombo
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.asUnsavedTrackCombos
import us.huseli.fistopy.dataclasses.track.mergeWith
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.OnConflictStrategy
import us.huseli.fistopy.enums.TrackMergeStrategy
import kotlin.math.min

interface IAlbumWithTracksCombo<A : IAlbum, T : IAlbumWithTracksCombo<A, T>> : IAlbumCombo<A> {
    class AlbumMatch<T>(
        val distance: Double,
        val albumCombo: T,
    )

    val tags: List<Tag>
    val trackCombos: List<ITrackCombo<*>>

    val discCount: Int
        get() = trackCombos.mapNotNull { it.track.discNumber }.maxOrNull() ?: 1

    val tracks: List<Track>
        get() = trackCombos.map { it.track }

    val trackIds: List<String>
        get() = trackCombos.map { it.track.trackId }

    override val minYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.minOrNull()

    override val maxYear: Int?
        get() = trackCombos.mapNotNull { it.track.year }.maxOrNull()

    override val isPartiallyDownloaded: Boolean
        get() = trackCombos.any { it.track.isDownloaded } && trackCombos.any { !it.track.isDownloaded }

    override val unplayableTrackCount: Int
        get() = trackCombos.count { !it.track.isPlayable }

    override val isDownloadable: Boolean
        get() = trackCombos.any { it.track.isDownloadable }

    fun getDistance(other: IAlbumWithTracksCombo<*, *>) = getDistance(
        other = other,
        missingOwnTracksPenalty = true,
        missingOtherTracksPenalty = false,
    )

    fun getDistance(
        other: IAlbumCombo<*>,
        missingOwnTracksPenalty: Boolean,
        missingOtherTracksPenalty: Boolean,
    ): Double {
        /**
         * Combines:
         *
         * 1. Combined Levenshtein distance for album properties
         * 2. Average Levenshtein distances for tracks
         * 3. If missingOwnTracksPenalty: 1.0 for each track missing from this
         * 4. If missingOtherTracksPenalty: 1.0 for each track missing from `other`
         *
         * They each have a relative weight of 1, except (2) which has a relative weight of 2.
         */
        val albumComboDistance = super.getDistance(other)

        if (other is IAlbumWithTracksCombo<*, *>) {
            val trackDistance = getTrackCombosDistance(other)
            val missingTracksPenalty =
                getMissingTracksPenalty(other, missingOwnTracksPenalty, missingOtherTracksPenalty)
            val baseWeight = 1.0 / (2 + listOf(missingOwnTracksPenalty, missingOtherTracksPenalty).filter { it }.size)

            return (trackDistance * baseWeight * 2) +
                (albumComboDistance * baseWeight) +
                (missingTracksPenalty * baseWeight * 2)
        }

        return albumComboDistance
    }

    @Suppress("UNCHECKED_CAST")
    fun match(other: IAlbumCombo<*>) =
        AlbumMatch<T>(distance = getDistance(other), albumCombo = this as T)

    private fun getMissingTracksPenalty(
        other: IAlbumWithTracksCombo<*, *>,
        missingOwnTracksPenalty: Boolean,
        missingOtherTracksPenalty: Boolean,
    ): Double {
        val missingOwnTracksDistance =
            if (missingOwnTracksPenalty && other.trackCombos.isNotEmpty()) (other.trackCombos.size - trackCombos.size)
                .coerceAtLeast(0)
                .toDouble()
                .div(other.trackCombos.size)
            else 0.0
        val missingOtherTracksDistance =
            if (missingOtherTracksPenalty && trackCombos.isNotEmpty()) (trackCombos.size - other.trackCombos.size)
                .coerceAtLeast(0)
                .toDouble()
                .div(trackCombos.size)
            else 0.0

        return missingOwnTracksDistance + missingOtherTracksDistance
    }

    private fun getTrackCombosDistance(other: IAlbumWithTracksCombo<*, *>): Double {
        val otherAlbumArtistNames = mutableListOf<String>()
        val otherAlbumArtistString = other.artists.joined()?.lowercase()

        otherAlbumArtistString?.also { otherAlbumArtistNames.add(it.lowercase()) }
        other.artists.forEach { otherAlbumArtistNames.add(it.name.lowercase()) }

        // Strip any "[artist] - " from our track titles, by other's artists:
        val thisTrackCombos = trackCombos.mapIndexed { index, trackCombo ->
            var title = trackCombo.track.title
            val otherTrackArtistNames = mutableListOf<String>()

            other.trackCombos.getOrNull(index)?.trackArtists?.run {
                joined()?.also { otherTrackArtistNames.add(it.lowercase()) }
                forEach { otherTrackArtistNames.add(it.name.lowercase()) }
            }
            for (artistName in otherTrackArtistNames + otherAlbumArtistNames) {
                title = title.replace(Regex("^$artistName( - *)?", RegexOption.IGNORE_CASE), "")
            }
            trackCombo.withTrack(trackCombo.track.copy(title = title))
        }

        return if (thisTrackCombos.isNotEmpty() || other.trackCombos.isNotEmpty()) thisTrackCombos
            .zip(other.trackCombos)
            .sumOf { (tt, ot) -> tt.getDistance(ot) }
            .toDouble()
            .div(min(thisTrackCombos.size, other.trackCombos.size))
        else 0.0
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class Builder(combo: IAlbumWithTracksCombo<*, *>) {
        var album = combo.album.asUnsavedAlbum()
            private set
        var artists = combo.artists
            private set
        var tags = combo.tags
            private set
        var trackCombos = combo.trackCombos.asUnsavedTrackCombos()
            private set

        fun build(): UnsavedAlbumWithTracksCombo = UnsavedAlbumWithTracksCombo(
            album = album,
            artists = artists,
            tags = tags,
            trackCombos = trackCombos,
        )

        fun mergeAlbum(other: IAlbum, onConflictStrategy: OnConflictStrategy = OnConflictStrategy.USE_OTHER) = apply {
            album = album.mergeWith(other, onConflictStrategy)
        }

        fun mergeArtists(
            other: List<IAlbumArtistCredit>,
            updateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        ) = apply {
            val albumArtists = other.map { it.withAlbumId(albumId = album.albumId) }.toMutableSet()

            if (updateStrategy == ListUpdateStrategy.MERGE) albumArtists.addAll(artists)
            artists = albumArtists.toList()
        }

        fun mergeTags(other: List<Tag>, updateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE) = apply {
            tags = when (updateStrategy) {
                ListUpdateStrategy.MERGE -> tags.toSet().plus(other).toList()
                ListUpdateStrategy.REPLACE -> other
            }
        }

        fun mergeTrackCombos(
            other: List<ITrackCombo<*>>,
            mergeStrategy: TrackMergeStrategy,
            artistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        ) = apply {
            trackCombos = trackCombos.mergeWith(other, mergeStrategy, artistUpdateStrategy).map { combo ->
                combo.copy(album = album, track = combo.track.copy(albumId = album.albumId))
            }
            updateTrackCount(trackCombos.size)
        }

        fun setAlbumArt(value: MediaStoreImage?) = updateAlbum { it.copy(albumArt = value) }

        fun setAlbumTitle(value: String) = updateAlbum { it.copy(title = value) }

        fun setIsInLibrary(value: Boolean) = apply {
            updateAlbum { it.copy(isInLibrary = value) }
            trackCombos = trackCombos.map { it.copy(track = it.track.copy(isInLibrary = value)) }
        }

        fun updateAlbum(callback: (UnsavedAlbum) -> UnsavedAlbum?) = apply {
            callback(album)?.also { album = it }
            trackCombos = trackCombos.map { it.copy(album = album, track = it.track.copy(albumId = album.albumId)) }
            artists = artists.map { it.withAlbumId(album.albumId) }
        }

        fun updateTracks(callback: (List<Track>) -> List<Track>) = apply {
            val newTracks = callback(trackCombos.map { it.track })

            trackCombos = trackCombos.zip(newTracks).map { (trackCombo, track) -> trackCombo.copy(track = track) }
            updateTrackCount(trackCombos.size)
        }

        private fun updateTrackCount(value: Int) {
            album = album.copy(trackCount = value)
        }
    }
}


inline fun IAlbumWithTracksCombo<*, *>.withUpdates(builder: IAlbumWithTracksCombo.Builder.() -> Unit): UnsavedAlbumWithTracksCombo {
    return IAlbumWithTracksCombo.Builder(this).apply(builder).build()
}
