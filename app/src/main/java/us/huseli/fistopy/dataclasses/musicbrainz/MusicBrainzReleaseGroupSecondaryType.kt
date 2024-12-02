package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName

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