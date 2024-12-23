package us.huseli.fistopy.repositories

import androidx.room.withTransaction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.dataclasses.playlist.Playlist
import us.huseli.fistopy.dataclasses.playlist.PlaylistUiState
import us.huseli.fistopy.dataclasses.track.PlaylistTrack
import us.huseli.fistopy.dataclasses.track.PlaylistTrackCombo
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.dataclasses.track.toUiStates
import us.huseli.fistopy.interfaces.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(private val database: Database) : AbstractScopeHolder(), ILogger {
    private val playlistDao = database.playlistDao()

    private var deletedPlaylist: Playlist? = null
    private var deletedPlaylistTracks: List<PlaylistTrack> = emptyList()

    val playlistUiStates: Flow<ImmutableList<PlaylistUiState>> = playlistDao.flowUiStates().map { it.toImmutableList() }

    suspend fun addTracksToPlaylist(
        playlistId: String,
        trackIds: Collection<String>,
        includeDuplicates: Boolean = true,
    ): Int = onIOThread {
        database.withTransaction {
            val currentTrackIds = playlistDao.listPlaylistTrackIds(playlistId).toSet()
            val toAdd =
                if (!includeDuplicates) trackIds.minus(currentTrackIds)
                else trackIds
            val playlistTracks = toAdd.mapIndexed { index, trackId ->
                PlaylistTrack(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = index + currentTrackIds.size,
                )
            }

            if (playlistTracks.isNotEmpty()) {
                playlistDao.insertPlaylistTracks(*playlistTracks.toTypedArray())
                playlistDao.touchPlaylist(playlistId)
            }

            playlistTracks.size
        }
    }

    suspend fun deleteOrphanPlaylistTracks() = onIOThread { playlistDao.deleteOrphanPlaylistTracks() }

    suspend fun deletePlaylist(playlistId: String) {
        onIOThread {
            playlistDao.getPlaylist(playlistId)?.also { playlist ->
                deletedPlaylist = playlist
                deletedPlaylistTracks = playlistDao.listPlaylistTracks(playlistId)
                playlistDao.deletePlaylist(playlistId)
            }
        }
    }

    fun flowPlaylistTrackUiStates(playlistId: String): Flow<ImmutableList<TrackUiState>> =
        playlistDao.flowTrackCombosByPlaylistId(playlistId).map { it.toUiStates() }

    suspend fun getDuplicatePlaylistTrackCount(playlistId: String, trackIds: Collection<String>): Int =
        onIOThread { playlistDao.getDuplicateTrackCount(playlistId, trackIds) }

    suspend fun getPlaylistName(playlistId: String): String? = playlistDao.getPlaylistName(playlistId)

    suspend fun insertPlaylist(playlist: Playlist) = onIOThread { playlistDao.insertPlaylists(playlist) }

    suspend fun insertPlaylistWithTracks(playlist: Playlist, trackIds: Collection<String>) {
        onIOThread { playlistDao.insertPlaylists(playlist) }
        if (trackIds.isNotEmpty()) addTracksToPlaylist(playlist.playlistId, trackIds)
    }

    suspend fun listPlaylistTrackCombos(playlistId: String): List<PlaylistTrackCombo> =
        onIOThread { playlistDao.listTrackCombosByPlaylistId(playlistId) }

    suspend fun movePlaylistTrack(playlistId: String, from: Int, to: Int) =
        onIOThread { playlistDao.moveTrack(playlistId, from, to) }

    suspend fun removePlaylistTracks(playlistId: String, playlistTrackIds: Collection<String>) {
        if (playlistTrackIds.isNotEmpty()) onIOThread {
            database.withTransaction {
                playlistDao.deletePlaylistTracks(*playlistTrackIds.toTypedArray())
                playlistDao.touchPlaylist(playlistId)
            }
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) =
        onIOThread { playlistDao.renamePlaylist(playlistId, newName) }

    fun undoDeletePlaylist(onFinish: (String) -> Unit) {
        launchOnIOThread {
            deletedPlaylist?.also { playlist ->
                insertPlaylist(playlist)
                if (deletedPlaylistTracks.isNotEmpty()) {
                    onIOThread { playlistDao.insertPlaylistTracks(*deletedPlaylistTracks.toTypedArray()) }
                }
                deletedPlaylist = null
                deletedPlaylistTracks = emptyList()
                onFinish(playlist.playlistId)
            }
        }
    }
}
