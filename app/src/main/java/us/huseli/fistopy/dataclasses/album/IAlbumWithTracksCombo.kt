package us.huseli.fistopy.dataclasses.album

import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.tag.AlbumTag
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.tag.toAlbumTags
import us.huseli.fistopy.dataclasses.track.ITrackCombo
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.UnsavedTrackCombo
import us.huseli.fistopy.enums.ListUpdateStrategy
import kotlin.math.max

interface IAlbumWithTracksCombo<out A : IAlbum> : IAlbumCombo<A> {
    val tags: List<Tag>
    val trackCombos: List<ITrackCombo>

    val albumTags: List<AlbumTag>
        get() = tags.toAlbumTags(album.albumId)

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

    class Builder(combo: IAlbumWithTracksCombo<IAlbum>) {
        private var album = combo.album.asUnsavedAlbum()
        private var artists = combo.artists
        private var tags = combo.tags
        private var trackCombos = combo.trackCombos

        fun build(): UnsavedAlbumWithTracksCombo = UnsavedAlbumWithTracksCombo(
            album = album.asUnsavedAlbum(),
            artists = artists,
            tags = tags,
            trackCombos = trackCombos,
        )

        fun mergeAlbum(other: IAlbum): Builder {
            album = album.copy(
                albumArt = other.albumArt ?: album.albumArt,
                albumType = other.albumType ?: album.albumType,
                musicBrainzReleaseGroupId = other.musicBrainzReleaseGroupId ?: album.musicBrainzReleaseGroupId,
                musicBrainzReleaseId = other.musicBrainzReleaseId ?: album.musicBrainzReleaseId,
                spotifyId = other.spotifyId ?: album.spotifyId,
                spotifyImage = other.spotifyImage ?: album.spotifyImage,
                youtubePlaylist = other.youtubePlaylist ?: album.youtubePlaylist,
            )
            return this
        }

        fun mergeArtists(
            other: List<IAlbumArtistCredit>,
            updateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        ): Builder {
            val albumArtists = other.map { it.withAlbumId(albumId = album.albumId) }.toMutableSet()

            if (updateStrategy == ListUpdateStrategy.MERGE) albumArtists.addAll(artists)
            artists = albumArtists.toList()
            return this
        }

        fun mergeTags(other: List<Tag>, updateStrategy: ListUpdateStrategy = ListUpdateStrategy.MERGE): Builder {
            tags = when (updateStrategy) {
                ListUpdateStrategy.MERGE -> tags.toSet().plus(other).toList()
                ListUpdateStrategy.REPLACE -> other
            }
            return this
        }

        fun mergeTrackCombos(
            other: List<ITrackCombo>,
            mergeStrategy: TrackMergeStrategy,
            artistUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        ): Builder {
            val mergedTrackCombos = mutableListOf<ITrackCombo>()

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
            album = album.copy(trackCount = mergedTrackCombos.size)
            return this
        }

        fun setAlbumArt(value: MediaStoreImage?): Builder {
            album = album.copy(albumArt = value)
            return this
        }

        fun setIsInLibrary(value: Boolean): Builder {
            album = album.copy(isInLibrary = value)
            trackCombos = trackCombos.map { it.withTrack(it.track.copy(isInLibrary = value)) }
            return this
        }
    }
}


inline fun IAlbumWithTracksCombo<IAlbum>.withUpdates(builder: IAlbumWithTracksCombo.Builder.() -> Unit): UnsavedAlbumWithTracksCombo {
    return IAlbumWithTracksCombo.Builder(this).apply(builder).build()
}
