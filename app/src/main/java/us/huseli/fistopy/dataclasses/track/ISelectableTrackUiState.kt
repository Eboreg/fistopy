package us.huseli.fistopy.dataclasses.track

import us.huseli.fistopy.interfaces.ISelectableItem

interface ISelectableTrackUiState : ISelectableItem {
    val trackId: String
}
