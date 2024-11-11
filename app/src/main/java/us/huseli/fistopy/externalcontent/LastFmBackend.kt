package us.huseli.fistopy.externalcontent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.dataclasses.album.ExternalAlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.lastfm.LastFmAlbum
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.fistopy.repositories.Repositories

class LastFmBackend(private val repos: Repositories) : IExternalImportBackend {
    override val albumImportHolder = object : AbstractAlbumImportHolder<LastFmAlbum>() {
        override val isTotalCountExact: Flow<Boolean> = flowOf(false)
        override val canImport: Flow<Boolean> = repos.lastFm.username.map { it != null }

        override suspend fun getAlbumWithTracks(albumId: String): ExternalAlbumWithTracksCombo<*>? {
            return externalAlbums[albumId]?.let { combo ->
                repos.musicBrainz.getRelease(combo.externalData.mbid)?.let { release ->
                    val albumArt = repos.musicBrainz
                        .getCoverArtArchiveImage(
                            releaseId = combo.externalData.mbid,
                            releaseGroupId = combo.externalData.id,
                        )
                        ?.toMediaStoreImage()

                    release.toAlbumWithTracks(albumId = albumId, albumArt = albumArt)
                }
            }
        }

        override fun getExternalAlbumChannel() = Channel<ExternalAlbumWithTracksCombo<LastFmAlbum>>().also { channel ->
            launchOnIOThread {
                repos.lastFm.username.collectLatest { username ->
                    _items.value = emptyList()
                    _allItemsFetched.value = false
                    if (username != null) {
                        for (lastFmAlbum in repos.lastFm.topAlbumsChannel()) {
                            channel.send(lastFmAlbum.toAlbumWithTracks())
                        }
                    }
                    _allItemsFetched.value = true
                }
            }
        }

        override suspend fun getPreviouslyImportedIds(): List<String> = repos.lastFm.listMusicBrainzReleaseIds()
    }
}
