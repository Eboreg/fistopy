package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.IAlbumProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum
import us.huseli.fistopy.enums.AlbumType

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseGroupPrimaryType(val sortPrio: Int) {
    @SerializedName("Album") ALBUM(0),
    @SerializedName("Single") SINGLE(2),
    @SerializedName("EP") EP(1),
    @SerializedName("Broadcast") BROADCAST(3),
    @SerializedName("Other") OTHER(4);

    val albumType: AlbumType?
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            EP -> AlbumType.EP
            else -> null
        }
}

@Suppress("unused", "IncorrectFormatting")
enum class MusicBrainzReleaseGroupSecondaryType {
    @SerializedName("Compilation") COMPILATION,
    @SerializedName("Soundtrack") SOUNDTRACK,
    @SerializedName("Spokenword") SPOKENWORD,
    @SerializedName("Interview") INTERVIEW,
    @SerializedName("Audiobook") AUDIOBOOK,
    @SerializedName("Audio drama") AUDIO_DRAMA,
    @SerializedName("Live") LIVE,
    @SerializedName("Remix") REMIX,
    @SerializedName("DJ-mix") DJ_MIX,
    @SerializedName("Mixtape/Street") MIXTAPE_STREET,
    @SerializedName("Demo") DEMO,
    @SerializedName("Field recording") FIELD_RECORDING,
}

abstract class AbstractMusicBrainzReleaseGroup : AbstractMusicBrainzItem(), IAlbumProducer {
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


    override fun toAlbum(isLocal: Boolean, isInLibrary: Boolean, albumId: String): UnsavedAlbum = UnsavedAlbum(
        albumId = albumId,
        albumType = albumType,
        isInLibrary = isInLibrary,
        isLocal = isLocal,
        musicBrainzReleaseGroupId = id,
        title = title,
        year = year,
    )
}

data class MusicBrainzSimplifiedReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    override val title: String,
    @SerializedName("secondary-types")
    override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>,
) : AbstractMusicBrainzReleaseGroup()

data class MusicBrainzReleaseGroup(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    @SerializedName("primary-type")
    override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
    val releases: List<MusicBrainzSimplifiedRelease>,
    override val title: String,
    @SerializedName("secondary-types")
    override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>,
) : AbstractMusicBrainzReleaseGroup()
