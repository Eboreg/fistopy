package us.huseli.fistopy.externalcontent.holders

import kotlinx.coroutines.channels.Channel
import us.huseli.fistopy.dataclasses.track.ExternalTrackCombo
import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.externalcontent.SearchParams
import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractTrackSearchHolder<T : IStringIdItem> : AbstractSearchHolder<TrackUiState>() {
    protected abstract fun getExternalTrackChannel(searchParams: SearchParams): Channel<ExternalTrackCombo<T>>

    override fun getResultChannel(searchParams: SearchParams) = Channel<TrackUiState>().also { channel ->
        launchOnIOThread {
            for (trackCombo in getExternalTrackChannel(searchParams)) {
                channel.send(trackCombo.toUiState())
            }
            channel.close()
        }
    }
}
