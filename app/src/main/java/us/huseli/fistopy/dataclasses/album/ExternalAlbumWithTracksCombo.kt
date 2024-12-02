package us.huseli.fistopy.dataclasses.album

import kotlinx.collections.immutable.toImmutableList
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.IArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.joined
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.IExternalTrackComboProducer
import us.huseli.fistopy.enums.ListUpdateStrategy
import us.huseli.fistopy.enums.OnConflictStrategy
import us.huseli.fistopy.interfaces.IStringIdItem

data class ExternalAlbumWithTracksCombo<T : IStringIdItem>(
    val externalData: T,
    val playCount: Int? = null,
    override val album: UnsavedAlbum,
    override val artists: List<IAlbumArtistCredit>,
    override val tags: List<Tag>,
    override val trackCombos: List<ExternalTrackCombo<*>>,
) : IAlbumWithTracksCombo<UnsavedAlbum, ExternalAlbumWithTracksCombo<T>> {
    override fun toImportableUiState(): ImportableAlbumUiState = album.toImportableUiState().copy(
        artistString = artists.joined(),
        artists = artists.toImmutableList(),
        isDownloadable = isDownloadable,
        playCount = playCount,
    )

    class Builder<AT : IStringIdItem>(
        album: IAlbum,
        private var externalData: AT,
        artists: Iterable<IArtistCredit> = emptyList(),
        tracks: Iterable<IExternalTrackComboProducer<*>> = emptyList(),
        tags: Iterable<Tag> = emptyList(),
        private var playCount: Int? = null,
    ) {
        private var album = album.asUnsavedAlbum()
        private var artists = artists.toList()
        private var trackCombos = tracks.map { it.toTrackCombo(album = this.album) }
        private var tags = tags.toList()

        val albumId: String
            get() = album.albumId

        fun build(): ExternalAlbumWithTracksCombo<AT> {
            val albumArtists = UnsavedAlbumArtistCredit.fromArtistCredits(artists, albumId)

            trackCombos = trackCombos.map {
                it.copy(
                    album = album,
                    track = it.track.copy(albumId = album.albumId, isInLibrary = album.isInLibrary),
                    albumArtists = albumArtists,
                )
            }

            return ExternalAlbumWithTracksCombo(
                album = album,
                artists = albumArtists,
                tags = tags,
                trackCombos = trackCombos,
                externalData = externalData,
                playCount = playCount,
            )
        }

        fun mergeArtists(
            other: List<IArtistCredit>,
            updateStrategy: ListUpdateStrategy = ListUpdateStrategy.REPLACE,
        ) = apply {
            val newArtists = other.toMutableSet()

            if (updateStrategy == ListUpdateStrategy.MERGE) newArtists.addAll(artists)
            artists = newArtists.toList()
        }

        fun mergeWithExistingAlbum(existingAlbum: IAlbum) = apply {
            album = album.mergeWith(existingAlbum, OnConflictStrategy.USE_THIS)
                .copy(albumId = existingAlbum.albumId)
        }

        fun setArtist(value: String) = apply {
            artists = listOf(UnsavedAlbumArtistCredit(name = value, albumId = album.albumId))
        }

        fun setArtists(value: List<IArtistCredit>) = apply {
            artists = UnsavedAlbumArtistCredit.fromArtistCredits(value, albumId = albumId)
        }

        fun setTrackCombos(value: Iterable<ExternalTrackCombo<*>>) = apply {
            trackCombos = value.mapIndexed { index, combo ->
                combo.copy(track = combo.track.copy(albumPosition = index + 1))
            }
        }
    }
}
