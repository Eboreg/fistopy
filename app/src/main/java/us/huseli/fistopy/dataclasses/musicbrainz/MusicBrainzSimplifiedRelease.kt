package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzSimplifiedRelease(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    override val country: String?,
    override val date: String?,
    override val id: String,
    override val status: MusicBrainzReleaseStatus?,
    override val title: String,
    override val trackCount: Int? = null,
    override val releaseGroupId: String? = null,
) : AbstractMusicBrainzRelease()