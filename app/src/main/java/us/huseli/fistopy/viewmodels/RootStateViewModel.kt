package us.huseli.fistopy.viewmodels

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.fistopy.dataclasses.album.Album
import us.huseli.fistopy.dataclasses.musicbrainz.MusicBrainzRelease
import us.huseli.fistopy.dataclasses.playlist.Playlist
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.managers.ExternalContentManager
import us.huseli.fistopy.managers.Managers
import us.huseli.fistopy.repositories.Repositories
import us.huseli.retaintheme.extensions.launchOnIOThread
import javax.inject.Inject

@HiltViewModel
class RootStateViewModel @Inject constructor(
    private val repos: Repositories,
    private val managers: Managers,
) : DownloadsViewModel(repos, managers), ExternalContentManager.Callback {
    private val _addToPlaylistTrackIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _albumToDownload = MutableStateFlow<Album?>(null)
    private val _editTrackState = MutableStateFlow<TrackUiState?>(null)
    private val _exportAlbumIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _exportPlaylistId = MutableStateFlow<String?>(null)
    private val _exportTrackIds = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    private val _selectMusicBrainzReleaseAlbumId = MutableStateFlow<String?>(null)
    private val _showInfoTrackId = MutableStateFlow<String?>(null)

    val addToPlaylistTrackIds = _addToPlaylistTrackIds.asStateFlow()
    val albumImportData = MutableStateFlow<List<ExternalContentManager.AlbumImportData>?>(null)
    val albumToDownload = _albumToDownload.asStateFlow()
    val deleteAlbums = MutableStateFlow<ImmutableList<String>>(persistentListOf())
    val editAlbumId = MutableStateFlow<String?>(null)
    val editTrackState = _editTrackState.asStateFlow()
    val exportAlbumIds = _exportAlbumIds.asStateFlow()
    val exportPlaylistId = _exportPlaylistId.asStateFlow()
    val exportTrackIds = _exportTrackIds.asStateFlow()
    val showCreatePlaylistDialog = MutableStateFlow(false)
    val showLibraryRadioDialog = MutableStateFlow(false)

    val selectMusicBrainzReleases = _selectMusicBrainzReleaseAlbumId.map { albumId ->
        albumId?.let {
            repos.album.getAlbum(albumId)?.musicBrainzReleaseGroupId?.let {
                repos.musicBrainz.listReleasesByReleaseGroupId(it).toImmutableList()
            }
        }
    }.stateWhileSubscribed()

    val showInfoTrackCombo: StateFlow<TrackCombo?> = _showInfoTrackId.map { trackId ->
        trackId?.let {
            repos.track.getTrackComboById(it)?.let { combo ->
                combo.copy(track = managers.library.ensureTrackMetadata(combo.track))
            }
        }
    }.stateWhileSubscribed()

    init {
        managers.external.addCallback(this)
    }

    fun createPlaylist(name: String, onFinish: (String) -> Unit) {
        launchOnIOThread {
            val playlist = Playlist(name = name)
            repos.playlist.insertPlaylist(playlist)
            onFinish(playlist.playlistId)
        }
    }

    fun clearExports() {
        _exportTrackIds.value = persistentListOf()
        _exportAlbumIds.value = persistentListOf()
        _exportPlaylistId.value = null
    }

    fun selectMusicBrainzRelease(release: MusicBrainzRelease) = launchOnIOThread {
        _selectMusicBrainzReleaseAlbumId.value?.let { repos.album.getAlbumWithTracks(it) }?.also { oldCombo ->
            _selectMusicBrainzReleaseAlbumId.value = null
            managers.library.upsertAlbumWithTracks(repos.musicBrainz.updateAlbumComboFromRelease(oldCombo, release))
        }
    }

    fun setAlbumToDownloadId(albumId: String?) {
        launchOnIOThread { _albumToDownload.value = albumId?.let { repos.album.getAlbum(it) } }
    }

    fun setAddToPlaylistAlbumIds(albumIds: Collection<String>) {
        launchOnIOThread { _addToPlaylistTrackIds.value = repos.track.listTrackIdsByAlbumId(albumIds) }
    }

    fun setAddToPlaylistArtistId(artistId: String) {
        launchOnIOThread { _addToPlaylistTrackIds.value = repos.track.listTrackIdsByArtistId(artistId) }
    }

    fun setAddToPlaylistTrackIds(trackIds: Collection<String>) {
        _addToPlaylistTrackIds.value = trackIds.toImmutableList()
    }

    fun setEditTrackId(trackId: String?) {
        launchOnIOThread {
            _editTrackState.value = trackId?.let { repos.track.getTrackComboById(it)?.toUiState() }
        }
    }

    fun setExportAlbumIds(albumIds: Collection<String>) {
        _exportAlbumIds.value = albumIds.toImmutableList()
    }

    fun setExportAllTracks() {
        launchOnIOThread {
            _exportTrackIds.value = repos.track.listTrackIds().toImmutableList()
        }
    }

    fun setExportPlaylistId(playlistId: String) {
        _exportPlaylistId.value = playlistId
    }

    fun setExportTrackIds(trackIds: Collection<String>) {
        _exportTrackIds.value = trackIds.toImmutableList()
    }

    fun setLocalMusicUri(value: Uri) = repos.settings.setLocalMusicUri(value)

    fun setSelectMusicBrainzReleaseAlbumId(albumId: String?) {
        _selectMusicBrainzReleaseAlbumId.value = albumId
    }

    fun setShowInfoTrackId(trackId: String?) {
        _showInfoTrackId.value = trackId
    }

    override fun onAlbumImportFinished(data: List<ExternalContentManager.AlbumImportData>) {
        albumImportData.value = data
    }
}
