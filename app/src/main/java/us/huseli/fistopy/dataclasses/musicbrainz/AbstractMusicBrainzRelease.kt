package us.huseli.fistopy.dataclasses.musicbrainz

import android.content.Context
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.enums.AlbumType
import us.huseli.fistopy.enums.getRegionName
import java.util.UUID

abstract class AbstractMusicBrainzRelease : AbstractMusicBrainzItem() {
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

    // open val releaseGroupId: String? = null
    // open val trackCount: Int? = null

    abstract val releaseGroupId: String?
    abstract val trackCount: Int?

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
        // musicBrainzReleaseGroupId = releaseGroupId ?: this.releaseGroupId,
        musicBrainzReleaseGroupId = releaseGroupId,
        albumArt = albumArt,
    )
}
