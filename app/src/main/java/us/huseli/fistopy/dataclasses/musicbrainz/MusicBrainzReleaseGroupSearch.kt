package us.huseli.fistopy.dataclasses.musicbrainz

import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IExternalAlbumWithTracksProducer
import us.huseli.fistopy.dataclasses.album.UnsavedAlbum

data class MusicBrainzReleaseGroupSearch(
    val count: Int,
    val offset: Int,
    @SerializedName("release-groups") val releaseGroups: List<ReleaseGroup>,
) {
    data class ReleaseGroup(
        override val id: String,
        override val title: String,
        @SerializedName("first-release-date")
        override val firstReleaseDate: String?,
        @SerializedName("primary-type")
        override val primaryType: MusicBrainzReleaseGroupPrimaryType?,
        @SerializedName("artist-credit")
        override val artistCredit: List<MusicBrainzArtistCredit>,
        val releases: List<Release>,
        @SerializedName("secondary-types")
        override val secondaryTypes: List<MusicBrainzReleaseGroupSecondaryType>?,
    ) : AbstractMusicBrainzReleaseGroup(), IExternalAlbumWithTracksProducer<ReleaseGroup> {
        data class Release(
            override val id: String,
            val status: MusicBrainzReleaseStatus? = null,
        ) : AbstractMusicBrainzItem()

        fun getPreferredReleaseId(): String? {
            return releases.firstOrNull { it.status == MusicBrainzReleaseStatus.OFFICIAL }?.id
                ?: releases.firstOrNull()?.id
        }

        override fun toAlbumWithTracks(
            isLocal: Boolean,
            isInLibrary: Boolean,
            albumId: String,
        ): ExternalAlbumWithTracksCombo<ReleaseGroup> {
            val album = UnsavedAlbum(
                title = title,
                year = year,
                musicBrainzReleaseGroupId = id,
                albumType = albumType,
                isLocal = isLocal,
                isInLibrary = isInLibrary,
                albumId = albumId,
            )

            return ExternalAlbumWithTracksCombo(
                externalData = this,
                album = album,
                tags = emptyList(),
                trackCombos = emptyList(),
                artists = artistCredit.toNativeAlbumArtists(album.albumId),
            )
        }
    }
}
