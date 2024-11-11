package us.huseli.fistopy.externalcontent

import us.huseli.fistopy.externalcontent.holders.AbstractAlbumSearchHolder
import us.huseli.fistopy.externalcontent.holders.AbstractTrackSearchHolder

interface IExternalSearchBackend {
    val albumSearchHolder: AbstractAlbumSearchHolder<*>
    val trackSearchHolder: AbstractTrackSearchHolder<*>

    fun getSearchHolder(listType: ExternalListType) = when (listType) {
        ExternalListType.ALBUMS -> albumSearchHolder
        ExternalListType.TRACKS -> trackSearchHolder
    }
}
