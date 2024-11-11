package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.dataclasses.ID3Data
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.dataclasses.artist.IAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedAlbumArtistCredit
import us.huseli.fistopy.dataclasses.artist.UnsavedTrackArtistCredit
import us.huseli.fistopy.interfaces.IStringIdItem
import java.util.UUID

data class LocalImportableTrack(
    override val id: String = UUID.randomUUID().toString(),
    val title: String,
    val albumPosition: Int?,
    val metadata: TrackMetadata?,
    val localUri: String,
    val id3: ID3Data,
) : IExternalTrackComboProducer<LocalImportableTrack>, IStringIdItem {
    override fun toTrackCombo(
        isInLibrary: Boolean,
        album: UnsavedAlbum?,
        albumArtists: List<IAlbumArtistCredit>?,
        albumPosition: Int?
    ): ExternalTrackCombo<LocalImportableTrack> {
        val track = Track(
            trackId = id,
            title = title,
            isInLibrary = isInLibrary,
            albumId = album?.albumId,
            albumPosition = albumPosition ?: this.albumPosition,
            localUri = localUri,
            metadata = metadata,
            musicBrainzId = id3.musicBrainzTrackId,
            year = id3.year,
            discNumber = id3.discNumber,
        )
        val trackArtists = id3.artist?.let {
            listOf(UnsavedTrackArtistCredit(name = it, trackId = track.trackId))
        }

        return ExternalTrackCombo(
            externalData = this,
            album = album,
            track = track,
            trackArtists = trackArtists ?: emptyList(),
            albumArtists = albumArtists ?: id3.albumArtist?.let { artist ->
                album?.albumId?.let { albumId ->
                    listOf(UnsavedAlbumArtistCredit(name = artist, albumId = albumId))
                }
            } ?: emptyList(),
        )
    }
}
