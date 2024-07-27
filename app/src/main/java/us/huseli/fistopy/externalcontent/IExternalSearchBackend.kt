package us.huseli.fistopy.externalcontent

import us.huseli.fistopy.dataclasses.track.TrackUiState
import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractSearchHolder
import us.huseli.fistopy.interfaces.IExternalAlbum

interface IExternalSearchBackend<T : IExternalAlbum> {
    val albumSearchHolder: AbstractAlbumSearchHolder<T>
    val trackSearchHolder: AbstractSearchHolder<TrackUiState>

    fun getSearchHolder(listType: ExternalListType) = when (listType) {
        ExternalListType.ALBUMS -> albumSearchHolder
        ExternalListType.TRACKS -> trackSearchHolder
    }
}
