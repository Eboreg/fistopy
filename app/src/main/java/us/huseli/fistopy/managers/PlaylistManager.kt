package us.huseli.fistopy.managers

import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.dataclasses.playlist.Playlist
import us.huseli.fistopy.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistManager @Inject constructor(private val repos: Repositories) : AbstractScopeHolder() {
    fun addTracksToPlaylist(
        playlistId: String,
        trackIds: Collection<String>,
        includeDuplicates: Boolean = true,
        onPlaylistClick: () -> Unit,
    ) {
        launchOnIOThread {
            repos.track.addToLibrary(trackIds)
            val added = repos.playlist.addTracksToPlaylist(playlistId, trackIds, includeDuplicates)
            repos.message.onAddTracksToPlaylist(trackCount = added, onPlaylistClick = onPlaylistClick)
        }
    }

    fun createPlaylist(playlist: Playlist, addTracks: Collection<String>, onPlaylistClick: () -> Unit) {
        launchOnIOThread {
            repos.track.addToLibrary(addTracks)
            repos.playlist.insertPlaylistWithTracks(playlist, addTracks)
            if (addTracks.isNotEmpty()) {
                repos.message.onAddTracksToPlaylist(trackCount = addTracks.size, onPlaylistClick = onPlaylistClick)
            }
        }
    }

    fun deletePlaylist(playlistId: String, onGotoPlaylistClick: () -> Unit) {
        launchOnIOThread {
            repos.playlist.deletePlaylist(playlistId)
            repos.message.onDeletePlaylist(
                onUndoClick = {
                    repos.playlist.undoDeletePlaylist {
                        repos.message.onUndeletePlaylist(onGotoPlaylistClick = onGotoPlaylistClick)
                    }
                },
            )
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        launchOnIOThread { repos.playlist.renamePlaylist(playlistId, newName) }
    }
}
