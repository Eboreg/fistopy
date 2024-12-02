package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzRecording(
    @SerializedName("artist-credit")
    override val artistCredit: List<MusicBrainzArtistCredit>,
    @SerializedName("first-release-date")
    override val firstReleaseDate: String?,
    val genres: List<MusicBrainzGenre>,
    override val id: String,
    override val length: Int,
    override val title: String,
) : AbstractMusicBrainzRecording<MusicBrainzRecording>()
