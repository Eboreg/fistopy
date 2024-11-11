package us.huseli.fistopy.dataclasses.musicbrainz

import android.content.Context
import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbumProducer
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.enums.getRegionName
import java.util.UUID

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseStatus {
    @SerializedName("Official") OFFICIAL,
    @SerializedName("Promotion") PROMOTION,
    @SerializedName("Bootleg") BOOTLEG,
    @SerializedName("Pseudo-release") PSEUDO_RELEASE,
    @SerializedName("Withdrawn") WITHDRAWN,
    @SerializedName("Cancelled") CANCELLED,
}

abstract class AbstractMusicBrainzRelease : AbstractMusicBrainzItem(), IAlbumProducer {
    abstract val artistCredit: List<MusicBrainzArtistCredit>
    abstract val country: String?
    abstract val date: String?
    abstract val status: MusicBrainzReleaseStatus?
    abstract val title: String
    abstract override val id: String

    open val albumType: AlbumType?
        get() = if (artistCredit.map { it.name.lowercase() }.contains("various artists"))
            AlbumType.COMPILATION
        else null

    open val releaseGroupId: String? = null

    open val trackCount: Int? = null

    open val year: Int?
        get() = date
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()

    fun getCountryName(context: Context): String? = country?.let { context.getRegionName(it) }

    protected fun toAlbum(
        isLocal: Boolean = false,
        isInLibrary: Boolean = false,
        albumId: String = UUID.randomUUID().toString(),
        releaseGroupId: String? = null,
        albumArt: MediaStoreImage? = null,
    ): UnsavedAlbum = UnsavedAlbum(
        albumId = albumId,
        albumType = albumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseId = id,
        title = title,
        trackCount = trackCount,
        year = year,
        musicBrainzReleaseGroupId = releaseGroupId ?: this.releaseGroupId,
        albumArt = albumArt,
    )

    override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String): UnsavedAlbum = toAlbum(
        isLocal = isLocal,
        isInLibrary = isInLibrary,
        albumId = albumId,
        releaseGroupId = releaseGroupId,
        albumArt = null,
    )
}

data class MusicBrainzSimplifiedRelease(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    override val country: String?,
    override val date: String?,
    override val id: String,
    override val status: MusicBrainzReleaseStatus?,
    override val title: String,
) : AbstractMusicBrainzRelease()

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
) : AbstractMusicBrainzRelease(), IExternalAlbumWithTracksProducer<MusicBrainzRelease> {
    private val allGenres: List<MusicBrainzGenre> = genres
        .asSequence()
        .plus(releaseGroup?.genres ?: emptyList())
        .plus(media.flatMap { media -> media.tracks.flatMap { track -> track.recording.genres } })
        .groupBy { it }
        .map { (genre, instances) -> genre.copy(count = instances.sumOf { it.count }) }
        .sortedByDescending { it.count }

    val mediaFormat: String? = media.mapNotNull { it.format }.toSet().takeIf { it.isNotEmpty() }?.joinToString("/")

    override val albumType: AlbumType? = super.albumType ?: releaseGroup?.albumType
    override val releaseGroupId: String? = releaseGroup?.id
    override val trackCount: Int = media.sumOf { it.trackCount }
    override val year: Int? = releaseGroup?.year ?: super.year

    fun toAlbumWithTracks(
        isInLibrary: Boolean = false,
        isLocal: Boolean = false,
        albumId: String = UUID.randomUUID().toString(),
        albumArt: MediaStoreImage? = null,
        releaseGroupId: String? = null,
    ): ExternalAlbumWithTracksCombo<MusicBrainzRelease> {
        val album = toAlbum(
            isLocal = isLocal,
            isInLibrary = isInLibrary,
            albumId = albumId,
            releaseGroupId = releaseGroupId,
            albumArt = albumArt,
        )
        val albumArtists = artistCredit.toNativeAlbumArtists(albumId = album.albumId)

        return ExternalAlbumWithTracksCombo(
            album = album,
            artists = albumArtists,
            tags = allGenres.toInternal(),
            trackCombos = media.toTrackCombos(
                isInLibrary = isInLibrary,
                album = album,
                albumArtists = albumArtists,
            ),
            externalData = this,
        )
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
