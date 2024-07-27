package us.huseli.fistopy.dataclasses.radio

import us.huseli.fistopy.dataclasses.track.QueueTrackCombo

data class RadioTrackCombo(
    val queueTrackCombo: QueueTrackCombo,
    val spotifyId: String? = null,
    val localId: String? = null,
)
