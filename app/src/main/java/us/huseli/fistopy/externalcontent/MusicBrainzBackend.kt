package us.huseli.fistopy.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRecordingSearch
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRelease
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzReleaseGroupSearch
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractTrackSearchHolder
import us.huseli.fistopy.repositories.Repositories

class MusicBrainzBackend(private val repos: Repositories) : IExternalSearchBackend {
    override val albumSearchHolder = object : AbstractAlbumSearchHolder<MusicBrainzReleaseGroupSearch.ReleaseGroup>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<MusicBrainzRelease>? {
            return externalAlbums[albumId]?.let { combo ->
                combo.externalData.getPreferredReleaseId()?.let { releaseId ->
                    repos.musicBrainz.getRelease(releaseId)?.let { release ->
                        val albumArt = repos.musicBrainz
                            .getCoverArtArchiveImage(releaseId = releaseId, releaseGroupId = combo.externalData.id)
                            ?.toMediaStoreImage()

                        release.toAlbumWithTracks(albumId = albumId, albumArt = albumArt)
                    }
                }
            }
        }

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<ExternalAlbumWithTracksCombo<MusicBrainzReleaseGroupSearch.ReleaseGroup>> =
            repos.musicBrainz.releaseGroupSearchChannel(searchParams)

        override suspend fun loadExistingAlbumCombos(): Map<String, AlbumCombo> =
            repos.album.mapAlbumCombosBySearchBackend(SearchBackend.MUSICBRAINZ)
    }

    override val trackSearchHolder = object : AbstractTrackSearchHolder<MusicBrainzRecordingSearch.Recording>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.TRACK, SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override fun getExternalTrackChannel(searchParams: SearchParams) =
            repos.musicBrainz.recordingSearchChannel(searchParams)
    }
}
