package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.interfaces.ILogger
import java.util.UUID

data class MusicBrainzRelease(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    override val country: String?,
    override val date: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    val media: List<MusicBrainzMedia>,
    @SerializedName("release-group")
    val releaseGroup: MusicBrainzSimplifiedReleaseGroup?,
    override val status: MusicBrainzReleaseStatus?,
    override val title: String,
    @SerializedName("label-info")
    val labelInfo: List<MusicBrainzLabelInfo>?,
) : AbstractMusicBrainzRelease(), IExternalAlbumWithTracksProducer<MusicBrainzRelease>, ILogger {
    private val allGenres: List<MusicBrainzGenre>
        get() = genres
            .asSequence()
            .plus(releaseGroup?.genres ?: emptyList())
            .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
            .groupBy { it }
            .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
            .sortedByDescending { it.count }
    val mediaFormat: String?
        get() = media.mapNotNull { it.format }.toSet().takeIf { it.isNotEmpty() }?.joinToString("/")
    override val albumType: AlbumType?
        get() = super.albumType ?: releaseGroup?.albumType
    override val releaseGroupId: String?
        get() = releaseGroup?.id
    override val trackCount: Int
        get() = media.sumOf { it.trackCount }
    override val year: Int?
        get() = releaseGroup?.year ?: super.year

    fun toAlbumWithTracks(
        isInLibrary: Boolean = false,
        isLocal: Boolean = false,
        albumId: String = UUID.randomUUID().toString(),
        albumArt: MediaStoreImage? = null,
        releaseGroupId: String? = null,
    ): ExternalAlbumWithTracksCombo<MusicBrainzRelease> {
        val builder = ExternalAlbumWithTracksCombo.Builder(
            album = toAlbum(
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                albumId = albumId,
                releaseGroupId = releaseGroupId ?: this.releaseGroup?.id,
                albumArt = albumArt,
            ),
            externalData = this,
            tags = allGenres.toInternal(),
        )
            .setArtists(artistCredit.toNativeArtists())
            .setTrackCombos(media.toTrackCombos(isInLibrary = isInLibrary))

        return builder.build()
    }

    override fun toAlbumWithTracks(
        isLocal: Boolean,
        isInLibrary: Boolean,
        albumId: String,
    ): ExternalAlbumWithTracksCombo<MusicBrainzRelease> = toAlbumWithTracks(
        isLocal = isLocal,
        isInLibrary = isInLibrary,
        albumId = albumId,
        albumArt = null,
    )
}
