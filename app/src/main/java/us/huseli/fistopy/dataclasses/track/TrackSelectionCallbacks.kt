package us.huseli.fistopy.dataclasses.track

data class TrackSelectionCallbacks(
    val onAddToPlaylistClick: () -> Unit,
    val onEnqueueClick: (() -> Unit)? = null,
    val onExportClick: () -> Unit,
    val onPlayClick: (() -> Unit)? = null,
    val onSelectAllClick: () -> Unit,
    val onUnselectAllClick: () -> Unit,
)
