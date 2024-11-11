package us.huseli.fistopy.repositories

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.album.AlbumCombo
import us.huseli.fistopy.dataclasses.album.AlbumUiState
import us.huseli.fistopy.dataclasses.album.AlbumWithTracksCombo
import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.album.toUiStates
import us.huseli.fistopy.dataclasses.tag.Tag
import us.huseli.fistopy.enums.AlbumSortParameter
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.SortOrder
import us.huseli.fistopy.externalcontent.SearchBackend
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(database: Database) : AbstractScopeHolder() {
    private val albumDao = database.albumDao()

    suspend fun clearAlbumArt(albumId: String) = onIOThread { albumDao.clearAlbumArt(albumId) }

    suspend fun clearAlbums() = onIOThread { albumDao.clearAlbums() }

    suspend fun clearTags() = onIOThread { albumDao.clearTags() }

    suspend fun deleteTempAlbums() = onIOThread { albumDao.deleteTempAlbums() }

    fun flowAlbumCombo(albumId: String): Flow<AlbumCombo?> = albumDao.flowAlbumCombo(albumId)

    fun flowAlbumUiStates(
        sortParameter: AlbumSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: Collection<String>,
        availabilityFilter: AvailabilityFilter,
    ): Flow<ImmutableList<AlbumUiState>> = albumDao
        .flowAlbumCombos(
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            searchTerm = searchTerm,
            tagNames = tagNames,
            availabilityFilter = availabilityFilter,
        )
        .map { it.toUiStates() }

    fun flowAlbumUiStatesByArtist(artistId: String): Flow<ImmutableList<AlbumUiState>> =
        albumDao.flowAlbumCombosByArtist(artistId).map { it.toUiStates() }

    fun flowAlbumWithTracks(albumId: String): Flow<AlbumWithTracksCombo?> = albumDao.flowAlbumWithTracks(albumId)

    fun flowTagNamesByAlbumId(albumId: String): Flow<List<String>> = albumDao.flowTagNamesByAlbumId(albumId)

    fun flowTagPojos(availabilityFilter: AvailabilityFilter) = albumDao.flowTagPojos(availabilityFilter)

    fun flowTags(): Flow<List<Tag>> = albumDao.flowTags()

    suspend fun getAlbum(albumId: String): Album? = onIOThread { albumDao.getAlbum(albumId) }

    suspend fun getAlbumCombo(albumId: String): AlbumCombo? = onIOThread { albumDao.getAlbumCombo(albumId) }

    suspend fun getAlbumWithTracks(albumId: String): AlbumWithTracksCombo? =
        onIOThread { albumDao.getAlbumWithTracks(albumId) }

    suspend fun getAlbumWithTracksByYoutubePlaylistId(playlistId: String) =
        onIOThread { albumDao.getAlbumWithTracksByYoutubePlaylistId(playlistId) }

    suspend fun getOrCreateAlbumByMusicBrainzId(album: IAlbum, groupId: String, releaseId: String): Album =
        onIOThread { albumDao.getOrCreateAlbumByMusicBrainzId(album, groupId, releaseId) }

    suspend fun getOrCreateAlbumBySpotifyId(album: IAlbum, spotifyId: String): Album =
        onIOThread { albumDao.getOrCreateAlbumBySpotifyId(album, spotifyId) }

    suspend fun insertTags(tags: Collection<Tag>) {
        if (tags.isNotEmpty()) onIOThread { albumDao.insertTags(*tags.toTypedArray()) }
    }

    suspend fun listAlbumCombos(): List<AlbumCombo> = onIOThread { albumDao.listAlbumCombos() }

    suspend fun listAlbumUiStates(albumIds: Collection<String>): List<AlbumUiState> =
        if (albumIds.isNotEmpty()) onIOThread { albumDao.listAlbumCombos(*albumIds.toTypedArray()).toUiStates() }
        else emptyList()

    suspend fun listAlbumIds(): List<String> = onIOThread { albumDao.listAlbumIds() }

    suspend fun listAlbumsWithTracks(): List<AlbumWithTracksCombo> = onIOThread { albumDao.listAlbumsWithTracks() }

    suspend fun listAlbumsWithTracks(albumIds: Collection<String>): List<AlbumWithTracksCombo> =
        if (albumIds.isNotEmpty()) onIOThread { albumDao.listAlbumsWithTracks(*albumIds.toTypedArray()) }
        else emptyList()

    suspend fun listTagNames(): List<String> = onIOThread { albumDao.listTagNames() }

    suspend fun listTags(albumId: String): List<Tag> = onIOThread { albumDao.listTags(albumId) }

    suspend fun mapAlbumCombosBySearchBackend(backend: SearchBackend): Map<String, AlbumCombo> {
        return when (backend) {
            SearchBackend.YOUTUBE -> albumDao.mapYoutubeAlbumCombos()
            SearchBackend.SPOTIFY -> albumDao.mapSpotifyAlbumCombos()
            SearchBackend.MUSICBRAINZ -> albumDao.mapMusicBrainzReleaseGroupAlbumCombos()
        }
    }

    suspend fun setAlbumsIsHidden(albumIds: Collection<String>, value: Boolean) =
        onIOThread { albumDao.setIsHidden(value, *albumIds.toTypedArray()) }

    suspend fun setAlbumsIsLocal(albumIds: Collection<String>, isLocal: Boolean) {
        if (albumIds.isNotEmpty()) onIOThread { albumDao.setIsLocal(isLocal, *albumIds.toTypedArray()) }
    }

    suspend fun setAlbumTags(albumId: String, tags: Collection<Tag>) =
        onIOThread { albumDao.setAlbumTags(albumId, tags) }

    suspend fun unhideLocalAlbums() = onIOThread { albumDao.unhideLocalAlbums() }

    suspend fun updateAlbumArt(albumId: String, albumArt: MediaStoreImage?) {
        onIOThread {
            if (albumArt != null) albumDao.updateAlbumArt(albumId, albumArt)
            else albumDao.clearAlbumArt(albumId)
        }
    }

    suspend fun upsertAlbum(album: IAlbum) = onIOThread { albumDao.upsertAlbum(album) }
}
