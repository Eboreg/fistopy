package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ITrackCombo
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.UnsavedTrackCombo
import us.huseli.fistopy.dataclasses.track.asUnsavedTrackCombos
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.OnConflictStrategy
import us.huseli.fistopy.enums.TrackMergeStrategy
import kotlin.math.max
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
        private var album = combo.album.asUnsavedAlbum()
        private var artists = combo.artists
        private var tags = combo.tags
        private var trackCombos = combo.trackCombos.asUnsavedTrackCombos()

        fun build(): UnsavedAlbumWithTracksCombo = UnsavedAlbumWithTracksCombo(
            album = album.asUnsavedAlbum(),
            artists = artists,
            tags = tags,
            trackCombos = trackCombos,
        )

        fun mergeAlbum(other: IAlbum, onConflictStrategy: OnConflictStrategy = OnConflictStrategy.USE_OTHER) = apply {
            album = when (onConflictStrategy) {
                OnConflictStrategy.USE_THIS -> album.copy(
                    albumArt = album.albumArt ?: other.albumArt,
                    albumType = album.albumType ?: other.albumType,
                    musicBrainzReleaseId = album.musicBrainzReleaseId ?: other.musicBrainzReleaseId,
                    musicBrainzReleaseGroupId = album.musicBrainzReleaseGroupId ?: other.musicBrainzReleaseGroupId,
                    spotifyId = album.spotifyId ?: other.spotifyId,
                    youtubePlaylist = album.youtubePlaylist ?: other.youtubePlaylist,
                )
                OnConflictStrategy.USE_OTHER -> album.copy(
                    albumArt = other.albumArt ?: album.albumArt,
                    albumType = other.albumType ?: album.albumType,
                    musicBrainzReleaseGroupId = other.musicBrainzReleaseGroupId ?: album.musicBrainzReleaseGroupId,
                    musicBrainzReleaseId = other.musicBrainzReleaseId ?: album.musicBrainzReleaseId,
                    spotifyId = other.spotifyId ?: album.spotifyId,
                    youtubePlaylist = other.youtubePlaylist ?: album.youtubePlaylist,
                )
            }
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
            val mergedTrackCombos = mutableListOf<UnsavedTrackCombo>()

            for (i in 0 until max(trackCombos.size, other.size)) {
                val thisTrackCombo = trackCombos.find { it.track.albumPosition == i + 1 }
                val otherTrackCombo = other.find { it.track.albumPosition == i + 1 }

                if (thisTrackCombo != null && otherTrackCombo != null) {
                    val trackArtists = otherTrackCombo.trackArtists
                        .map { it.withTrackId(trackId = thisTrackCombo.track.trackId) }
                        .toMutableSet()
                    if (artistUpdateStrategy == ListUpdateStrategy.MERGE) trackArtists.addAll(thisTrackCombo.trackArtists)

                    mergedTrackCombos.add(
                        UnsavedTrackCombo(
                            track = otherTrackCombo.track.copy(
                                musicBrainzId = otherTrackCombo.track.musicBrainzId
                                    ?: thisTrackCombo.track.musicBrainzId,
                                trackId = thisTrackCombo.track.trackId,
                                albumId = thisTrackCombo.track.albumId,
                                localUri = otherTrackCombo.track.localUri ?: thisTrackCombo.track.localUri,
                                spotifyId = otherTrackCombo.track.spotifyId ?: thisTrackCombo.track.spotifyId,
                                youtubeVideo = otherTrackCombo.track.youtubeVideo ?: thisTrackCombo.track.youtubeVideo,
                                metadata = otherTrackCombo.track.metadata ?: thisTrackCombo.track.metadata,
                                image = otherTrackCombo.track.image ?: thisTrackCombo.track.image,
                                durationMs = otherTrackCombo.track.durationMs ?: thisTrackCombo.track.durationMs,
                            ),
                            album = album,
                            trackArtists = trackArtists.toList(),
                            albumArtists = otherTrackCombo.albumArtists,
                        )
                    )
                } else if (
                    thisTrackCombo != null &&
                    (mergeStrategy == TrackMergeStrategy.KEEP_SELF || mergeStrategy == TrackMergeStrategy.KEEP_MOST)
                ) {
                    mergedTrackCombos.add(thisTrackCombo)
                } else if (
                    otherTrackCombo != null &&
                    (mergeStrategy == TrackMergeStrategy.KEEP_OTHER || mergeStrategy == TrackMergeStrategy.KEEP_MOST)
                ) {
                    mergedTrackCombos.add(
                        UnsavedTrackCombo(
                            track = otherTrackCombo.track.copy(albumId = album.albumId),
                            album = album,
                            trackArtists = otherTrackCombo.trackArtists,
                            albumArtists = otherTrackCombo.albumArtists,
                        )
                    )
                }
            }

            trackCombos = mergedTrackCombos.toList()
            updateTrackCount(trackCombos.size)
        }

        fun setAlbumArt(value: MediaStoreImage?) = updateAlbum { it.copy(albumArt = value) }

        fun setAlbumTitle(value: String) = updateAlbum { it.copy(title = value) }

        fun setIsInLibrary(value: Boolean) = apply {
            updateAlbum { it.copy(isInLibrary = value) }
            updateEachTrack { it.copy(isInLibrary = value) }
        }

        fun updateAlbum(callback: (UnsavedAlbum) -> UnsavedAlbum) = apply {
            album = callback(album)
            updateEachTrackCombo { it.copy(album = album) }
        }

        fun updateEachTrack(callback: (Track) -> Track) = apply {
            trackCombos = trackCombos.map { it.copy(track = callback(it.track)) }
        }

        fun updateEachTrackCombo(callback: (UnsavedTrackCombo) -> UnsavedTrackCombo) = apply {
            trackCombos = trackCombos.map(callback)
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
