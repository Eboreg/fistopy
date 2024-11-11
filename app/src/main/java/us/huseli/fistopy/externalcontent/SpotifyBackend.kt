package us.huseli.fistopy.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.spotify.SpotifyAlbum
import us.huseli.fistopy.dataclasses.spotify.SpotifySimplifiedAlbum
import us.huseli.fistopy.dataclasses.spotify.SpotifyTrack
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractTrackSearchHolder
import us.huseli.fistopy.repositories.Repositories

class SpotifyBackend(private val repos: Repositories) : IExternalSearchBackend, IExternalImportBackend {
    override val albumImportHolder = object : AbstractAlbumImportHolder<SpotifyAlbum>() {
        override val canImport = repos.spotify.oauth2PKCE.isAuthorized
        override val totalItemCount: Flow<Int> = combine(
            searchTerm,
            repos.spotify.totalUserAlbumCount,
            _filteredItems,
        ) { searchTerm, totalCount, items ->
            if (searchTerm.isBlank()) totalCount ?: items.size
            else items.size
        }
        override val isTotalCountExact: Flow<Boolean> =
            combine(searchTerm, repos.spotify.allUserAlbumsFetched) { term, allFetched ->
                term == "" || allFetched
            }

        override fun getExternalAlbumChannel() =
            Channel<ExternalAlbumWithTracksCombo<SpotifyAlbum>>().also { channel ->
                launchOnIOThread {
                    repos.spotify.oauth2PKCE.isAuthorized.collectLatest { authorized ->
                        _items.value = emptyList()
                        _allItemsFetched.value = false
                        if (authorized) {
                            for (spotifyAlbum in repos.spotify.userAlbumsChannel()) {
                                channel.send(spotifyAlbum.toAlbumWithTracks())
                            }
                        }
                        _allItemsFetched.value = true
                    }
                }
            }

        override suspend fun getPreviouslyImportedIds(): List<String> = repos.spotify.listSpotifyAlbumIds()
    }

    override val albumSearchHolder = object : AbstractAlbumSearchHolder<SpotifySimplifiedAlbum>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(true)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<SpotifyAlbum>? {
            return externalAlbums[albumId]?.let {
                repos.spotify.getAlbum(it.externalData.id)?.toAlbumWithTracks(albumId = albumId)
            }
        }

        override fun getExternalAlbumChannel(searchParams: SearchParams): Channel<ExternalAlbumWithTracksCombo<SpotifySimplifiedAlbum>> =
            repos.spotify.albumSearchChannel(searchParams)

        override suspend fun loadExistingAlbumCombos(): Map<String, AlbumCombo> =
            repos.album.mapAlbumCombosBySearchBackend(SearchBackend.SPOTIFY)
    }

    override val trackSearchHolder = object : AbstractTrackSearchHolder<SpotifyTrack>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(true)
        override val searchCapabilities: List<SearchCapability> =
            listOf(SearchCapability.TRACK, SearchCapability.ALBUM, SearchCapability.ARTIST, SearchCapability.FREE_TEXT)

        override fun getExternalTrackChannel(searchParams: SearchParams): Channel<ExternalTrackCombo<SpotifyTrack>> =
            repos.spotify.trackSearchChannel(searchParams)
    }
}
