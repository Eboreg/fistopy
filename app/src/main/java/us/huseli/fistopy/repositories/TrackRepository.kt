package us.huseli.fistopy.repositories

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.anggrayudi.storage.extension.openInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import linc.com.amplituda.Amplituda
import linc.com.amplituda.Compress
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.dataclasses.tag.TagPojo
import us.huseli.fistopy.dataclasses.track.Track
import us.huseli.fistopy.dataclasses.track.TrackCombo
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.dataclasses.track.toUiStates
import us.huseli.fistopy.deleteWithEmptyParentDirs
import us.huseli.fistopy.enums.AvailabilityFilter
import us.huseli.fistopy.enums.SortOrder
import us.huseli.fistopy.enums.TrackSortParameter
import us.huseli.fistopy.interfaces.ILogger
import us.huseli.fistopy.isRemote
import us.huseli.fistopy.waveList
import us.huseli.retaintheme.extensions.pruneOrPad
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class TrackRepository @Inject constructor(
    database: Database,
    @ApplicationContext private val context: Context,
) : ILogger, AbstractScopeHolder() {
    private val trackDao = database.trackDao()

    suspend fun addToLibrary(trackIds: Collection<String>) =
        onIOThread { trackDao.setIsInLibrary(true, *trackIds.toTypedArray()) }

    suspend fun clearLocalUris(trackIds: Collection<String>) {
        if (trackIds.isNotEmpty()) onIOThread { trackDao.clearLocalUris(trackIds) }
    }

    suspend fun clearTracks() = onIOThread { trackDao.clearTracks() }

    suspend fun deleteTempTracks() = onIOThread { trackDao.deleteTempTracks() }

    @WorkerThread
    fun deleteTrackFiles(tracks: Collection<Track>) = tracks
        .mapNotNull { it.getDocumentFile(context) }
        .forEach { it.deleteWithEmptyParentDirs(context) }

    suspend fun deleteTracksById(trackIds: Collection<String>) {
        if (trackIds.isNotEmpty()) onIOThread { trackDao.deleteTracksById(*trackIds.toTypedArray()) }
    }

    fun flowAmplitudes(trackId: String): Flow<ImmutableList<Int>> = channelFlow {
        val track = trackDao.getTrackById(trackId)
        val amplitudeList = track?.amplitudeList
        val url = track?.lofiUri

        if (amplitudeList != null) send(amplitudeList)
        else {
            val waitingJob = launchOnIOThread {
                var offset = 0

                while (true) {
                    send(waveList(100, 0, 12, 3, offset).toImmutableList())
                    offset++
                    delay(200L)
                }
            }

            if (url != null) {
                val amplituda = Amplituda(context)
                val samplesPerSecond =
                    track.durationMs?.div(1000)?.let { if (it < 100) ceil(100.0 / it).toInt() else 1 } ?: 1
                val comp = Compress.withParams(Compress.PEEK, samplesPerSecond)
                val output = onIOThread {
                    url.toUri().takeIf { !it.isRemote }?.let { uri ->
                        uri.openInputStream(context)?.use { amplituda.processAudio(it, comp) }
                    } ?: amplituda.processAudio(url, comp)
                }
                var amplitudes: ImmutableList<Int>? = null

                output.get(
                    { result -> amplitudes = result.amplitudesAsList().pruneOrPad(100).toImmutableList() },
                    { exception -> logError("amplitudes $url", exception) },
                )
                waitingJob.cancel()
                amplitudes?.also {
                    send(it)
                    launchOnIOThread {
                        trackDao.setTrackAmplitudes(trackId, it.joinToString(","))
                    }
                }
            }
        }
    }

    fun flowTagPojos(availabilityFilter: AvailabilityFilter): Flow<List<TagPojo>> =
        trackDao.flowTagPojos(availabilityFilter)

    fun flowTrackCombosByAlbumId(albumId: String): Flow<List<TrackCombo>> = trackDao.flowTrackCombosByAlbumId(albumId)

    fun flowTrackUiStates(
        sortParameter: TrackSortParameter,
        sortOrder: SortOrder,
        searchTerm: String,
        tagNames: List<String>,
        availabilityFilter: AvailabilityFilter,
    ): Flow<ImmutableList<TrackUiState>> = trackDao
        .flowTrackCombos(
            sortParameter = sortParameter,
            sortOrder = sortOrder,
            searchTerm = searchTerm,
            tagNames = tagNames,
            availabilityFilter = availabilityFilter,
        )
        .map { it.toUiStates() }

    fun flowTrackUiStatesByArtist(artistId: String): Flow<ImmutableList<TrackUiState>> =
        trackDao.flowTrackCombosByArtist(artistId).map { it.toUiStates() }

    suspend fun getLibraryTrackCount(): Int = onIOThread { trackDao.getLibraryTrackCount() }

    fun getLocalAbsolutePath(track: Track): String? = track.getLocalAbsolutePath(context)

    suspend fun getTrackById(trackId: String): Track? = onIOThread { trackDao.getTrackById(trackId) }

    suspend fun getTrackComboById(trackId: String): TrackCombo? = onIOThread { trackDao.getTrackComboById(trackId) }

    suspend fun listNonAlbumTracks() = onIOThread { trackDao.listNonAlbumTracks() }

    suspend fun listRandomLibraryTrackCombos(
        limit: Int,
        exceptTrackIds: List<String> = emptyList(),
        exceptSpotifyTrackIds: List<String> = emptyList(),
    ): List<TrackCombo> = onIOThread {
        trackDao.listRandomLibraryTrackCombos(
            limit = limit,
            exceptTrackIds = exceptTrackIds,
            exceptSpotifyTrackIds = exceptSpotifyTrackIds,
        )
    }

    suspend fun listTrackCombosByAlbumId(vararg albumIds: String): List<TrackCombo> =
        onIOThread { trackDao.listTrackCombosByAlbumId(*albumIds) }

    suspend fun listTrackCombosByArtistId(artistId: String): List<TrackCombo> =
        onIOThread { trackDao.listTrackCombosByArtistId(artistId) }

    suspend fun listTrackCombosById(trackIds: Collection<String>): List<TrackCombo> =
        if (trackIds.isNotEmpty()) onIOThread { trackDao.listTrackCombosById(*trackIds.toTypedArray()) }
        else emptyList()

    suspend fun listTrackIds(): List<String> = onIOThread { trackDao.listTrackIds() }

    suspend fun listTrackIdsByAlbumId(albumIds: Collection<String>): ImmutableList<String> =
        onIOThread { trackDao.listTrackIdsByAlbumId(*albumIds.toTypedArray()).toImmutableList() }

    suspend fun listTrackIdsByArtistId(artistId: String): ImmutableList<String> =
        onIOThread { trackDao.listTrackIdsByArtistId(artistId).toImmutableList() }

    suspend fun listTrackLocalUris(): List<Uri> = onIOThread { trackDao.listLocalUris().map { it.toUri() } }

    suspend fun listTracksByAlbumId(albumId: String): List<Track> = onIOThread { trackDao.listTracksByAlbumId(albumId) }

    suspend fun setAlbumTracks(albumId: String, tracks: Collection<Track>) = onIOThread {
        trackDao.setAlbumTracks(albumId = albumId, tracks = tracks)
    }

    suspend fun setPlayCounts(trackId: String, localPlayCount: Int, lastFmPlayCount: Int?) =
        onIOThread { trackDao.setPlayCounts(trackId, localPlayCount, lastFmPlayCount) }

    suspend fun setTrackSpotifyId(trackId: String, spotifyId: String) =
        onIOThread { trackDao.setTrackSpotifyId(trackId, spotifyId) }

    suspend fun upsertTrack(track: Track) = onIOThread { trackDao.upsertTracks(track) }

    suspend fun upsertTracks(tracks: Collection<Track>) {
        if (tracks.isNotEmpty()) onIOThread { trackDao.upsertTracks(*tracks.toTypedArray()) }
    }
}
