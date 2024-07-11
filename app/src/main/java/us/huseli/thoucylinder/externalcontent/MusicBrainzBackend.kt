package us.huseli.thoucylinder.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import us.huseli.thoucylinder.dataclasses.album.AlbumCombo
import us.huseli.thoucylinder.dataclasses.album.IAlbum
import us.huseli.thoucylinder.dataclasses.album.IAlbumCombo
import us.huseli.thoucylinder.dataclasses.album.UnsavedAlbumWithTracksCombo
import us.huseli.thoucylinder.dataclasses.musicbrainz.MusicBrainzReleaseGroupSearch
import us.huseli.thoucylinder.dataclasses.track.TrackUiState
import us.huseli.thoucylinder.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.thoucylinder.externalcontent.holders.AbstractSearchHolder
import us.huseli.thoucylinder.repositories.Repositories

class MusicBrainzBackend(private val repos: Repositories) : IExternalSearchBackend<MusicBrainzReleaseGroupSearch.ReleaseGroup> {
    override val albumSearchHolder = object : AbstractAlbumSearchHolder<MusicBrainzReleaseGroupSearch.ReleaseGroup>() {
        private var _existingAlbumCombos: Map<String, AlbumCombo>? = null

        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        private suspend fun getExistingAlbumCombos(): Map<String, AlbumCombo> {
            return _existingAlbumCombos
                ?: repos.album.listMusicBrainzReleaseGroupAlbumCombos().also { _existingAlbumCombos = it }
        }

        override suspend fun convertToAlbumWithTracks(
            externalAlbum: MusicBrainzReleaseGroupSearch.ReleaseGroup,
            albumId: String,
        ): UnsavedAlbumWithTracksCombo? {
            return externalAlbum.getPreferredReleaseId()?.let { releaseId ->
                repos.musicBrainz.getRelease(releaseId)?.let { release ->
                    val albumArt = repos.musicBrainz
                        .getCoverArtArchiveImage(releaseId = releaseId, releaseGroupId = externalAlbum.id)
                        ?.toMediaStoreImage()

                    release.toAlbumWithTracks(
                        albumId = albumId,
                        isLocal = false,
                        isInLibrary = false,
                        albumArt = albumArt,
                    )
                }
            }
        }

        override suspend fun externalAlbumToAlbumCombo(externalAlbum: MusicBrainzReleaseGroupSearch.ReleaseGroup): IAlbumCombo<IAlbum> =
            getExistingAlbumCombos()[externalAlbum.id] ?: super.externalAlbumToAlbumCombo(externalAlbum)

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<MusicBrainzReleaseGroupSearch.ReleaseGroup> =
            repos.musicBrainz.releaseGroupSearchChannel(searchParams)
    }

    override val trackSearchHolder = object : AbstractSearchHolder<TrackUiState>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.TRACK, SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override fun getResultChannel(searchParams: SearchParams) = Channel<TrackUiState>().also { channel ->
            launchOnIOThread {
                for (recording in repos.musicBrainz.recordingSearchChannel(searchParams)) {
                    channel.send(recording.toTrackCombo(isInLibrary = false).toUiState())
                }
                channel.close()
            }
        }

    }
}
