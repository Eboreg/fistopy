package us.huseli.fistopy.managers

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import us.huseli.fistopy.AbstractScopeHolder
import us.huseli.fistopy.RadioTrackChannel
import us.huseli.fistopy.database.Database
import us.huseli.fistopy.dataclasses.radio.Radio
import us.huseli.fistopy.dataclasses.radio.RadioCombo
import us.huseli.fistopy.dataclasses.radio.RadioUiState
import us.huseli.fistopy.enums.RadioStatus
import us.huseli.fistopy.enums.RadioType
import us.huseli.fistopy.interfaces.ILogger
import us.huseli.fistopy.repositories.Repositories
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioManager @Inject constructor(
    database: Database,
    private val repos: Repositories,
) : AbstractScopeHolder(), ILogger {
    private var radioJob: Job? = null
    private var worker: RadioTrackChannel? = null

    private val radioDao = database.radioDao()
    private val _radioStatus = MutableStateFlow(RadioStatus.INACTIVE)

    val activeRadioCombo: Flow<RadioCombo?> = radioDao.flowActiveRadio()
        .filter { it == null || it.type == RadioType.LIBRARY || it.title != null }
        .distinctUntilChanged()
    val radioStatus = _radioStatus.asStateFlow()
    val radioUiState: Flow<RadioUiState?> = combine(activeRadioCombo, _radioStatus) { radio, status ->
        if (status != RadioStatus.INACTIVE) radio?.let { RadioUiState(type = it.type, title = it.title) } else null
    }

    init {
        launchOnIOThread {
            activeRadioCombo.filterNotNull().collect {
                worker?.cancel()
                radioJob?.cancel()
                radioJob = launchOnMainThread { startRadio(it) }
            }
        }
        launchOnIOThread {
            for (replaced in repos.player.replaceSignal) deactivateRadio()
        }
    }

    fun deactivateRadio() {
        worker?.cancel()
        worker = null
        radioJob?.cancel()
        radioJob = null
        _radioStatus.value = RadioStatus.INACTIVE
        launchOnIOThread { radioDao.clearRadios() }
    }

    fun startAlbumRadio(albumId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(albumId = albumId, type = RadioType.ALBUM)) }
    }

    fun startArtistRadio(artistId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(artistId = artistId, type = RadioType.ARTIST)) }
    }

    fun startLibraryRadio() {
        launchOnIOThread { radioDao.setActiveRadio(Radio(type = RadioType.LIBRARY)) }
    }

    fun startTrackRadio(trackId: String) {
        launchOnIOThread { radioDao.setActiveRadio(Radio(trackId = trackId, type = RadioType.TRACK)) }
    }


    /** PRIVATE METHODS ***********************************************************************************************/

    private suspend fun handleNextRadioTrack(worker: RadioTrackChannel, clearAndPlay: Boolean = false) {
        if (clearAndPlay) repos.player.clearQueue()

        val combo = onIOThread { worker.channel.receive() }

        if (clearAndPlay) repos.player.insertLastAndPlay(combo.queueTrackCombo)
        else repos.player.insertLast(combo.queueTrackCombo)
        if (combo.localId != null) radioDao.addLocalTrackId(worker.radio.id, combo.localId)
        if (combo.spotifyId != null) radioDao.addSpotifyTrackId(worker.radio.id, combo.spotifyId)
    }

    private suspend fun startRadio(radio: RadioCombo) {
        val worker = RadioTrackChannel(radio = radio, repos = repos)
        var firstTrack = true

        this.worker = worker
        _radioStatus.value = RadioStatus.LOADING

        try {
            if (!radio.isInitialized) {
                handleNextRadioTrack(worker = worker, clearAndPlay = true)
                radioDao.setIsInitialized(worker.radio.id)
                firstTrack = false
            }

            combineTransform(repos.player.trackCount, repos.player.tracksLeft) { trackCount, tracksLeft ->
                if (trackCount < 20 || tracksLeft < 5) {
                    if (_radioStatus.value == RadioStatus.LOADED) _radioStatus.value = RadioStatus.LOADING_MORE
                    emit(true)
                } else _radioStatus.value = RadioStatus.LOADED
            }.collectLatest {
                handleNextRadioTrack(worker = worker)
                firstTrack = false
            }
        } catch (e: ClosedReceiveChannelException) {
            deactivateRadio()
            if (firstTrack) repos.message.onRadioRecommendationsNotFound(radio.title)
        } catch (e: Throwable) {
            logError(e)
        }
    }
}
