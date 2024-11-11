package us.huseli.fistopy.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.YoutubeAndroidClient
import us.huseli.fistopy.YoutubeWebClient
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.youtube.YoutubePlaylist
import us.huseli.fistopy.dataclasses.youtube.YoutubeVideo
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractTrackSearchHolder
import us.huseli.fistopy.playlistSearchChannel
import us.huseli.fistopy.repositories.Repositories
import us.huseli.fistopy.videoSearchChannel

class YoutubeBackend(repos: Repositories) : IExternalSearchBackend, AbstractScopeHolder() {
    private val _playlistClient: StateFlow<YoutubeWebClient> = repos.youtube.region
        .map { YoutubeWebClient(region = it) }
        .stateEagerly(YoutubeWebClient(region = repos.youtube.region.value))
    private val _videoClient: StateFlow<YoutubeAndroidClient> = repos.youtube.region
        .map { YoutubeAndroidClient(region = it) }
        .stateEagerly(YoutubeAndroidClient(region = repos.youtube.region.value))

    override val albumSearchHolder = object : AbstractAlbumSearchHolder<YoutubePlaylist>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> = listOf(SearchCapability.FREE_TEXT)

        override suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<*>? {
            return externalAlbums[albumId]?.let {
                _playlistClient.value
                    .getPlaylistComboFromPlaylistId(playlistId = it.externalData.id, artist = it.externalData.artist)
                    ?.toAlbumWithTracks(albumId = albumId)
            }
        }

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<ExternalAlbumWithTracksCombo<YoutubePlaylist>> =
            playlistSearchChannel(params = searchParams, client = _playlistClient.value, scope = scope)

        override suspend fun loadExistingAlbumCombos(): Map<String, AlbumCombo> =
            repos.album.mapAlbumCombosBySearchBackend(SearchBackend.YOUTUBE)
    }

    override val trackSearchHolder = object : AbstractTrackSearchHolder<YoutubeVideo>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val searchCapabilities: List<SearchCapability> = listOf(SearchCapability.FREE_TEXT)

        override fun getExternalTrackChannel(searchParams: SearchParams): Channel<ExternalTrackCombo<YoutubeVideo>> =
            videoSearchChannel(params = searchParams, client = _videoClient.value, scope = scope)
    }
}
