package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

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
