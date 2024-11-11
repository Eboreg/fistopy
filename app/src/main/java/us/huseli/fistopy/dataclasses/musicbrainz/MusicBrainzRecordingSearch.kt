package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

data class MusicBrainzRecordingSearch(
    val count: Int,
    val offset: Int,
    val recordings: List<Recording>,
) {
    data class Recording(
        override val id: String,
        override val title: String,
        @SerializedName("artist-credit")
        override val artistCredit: List<MusicBrainzArtistCredit>,
        override val length: Int,
        @SerializedName("first-release-date")
        override val firstReleaseDate: String?,
    ) : AbstractMusicBrainzRecording<Recording>()
}
