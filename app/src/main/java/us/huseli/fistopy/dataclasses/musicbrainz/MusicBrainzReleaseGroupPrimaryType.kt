package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
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