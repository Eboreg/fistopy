package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.enums.AlbumType

abstract class AbstractMusicBrainzReleaseGroup : AbstractMusicBrainzItem() {
    abstract val firstReleaseDate: String?
    abstract val primaryType: MusicBrainzReleaseGroupPrimaryType?
    abstract val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>?
    abstract val artistCredit: List<MusicBrainzArtistCredit>?

    abstract override val id: String
    abstract val title: String

    val albumType: AlbumType?
        get() {
            if (secondaryTypes?.contains(MusicBrainzReleaseGroupSecondaryType.COMPILATION) == true)
                return AlbumType.COMPILATION
            return primaryType?.albumType
        }

    val year: Int?
        get() = firstReleaseDate
            ?.substringBefore('-')
            ?.takeIf { it.matches(Regex("^\\d{4}$")) }
            ?.toInt()


    fun toAlbum(isLocal: Boolean = false, isInLibrary: Boolean = false): UnsavedAlbum =
        UnsavedAlbum(
            albumType = albumType,
            isInLibrary = isInLibrary,
            isLocal = isLocal,
            musicBrainzReleaseGroupId = id,
            title = title,
            year = year,
        )
}
