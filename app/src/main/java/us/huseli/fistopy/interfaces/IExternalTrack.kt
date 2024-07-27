package us.huseli.fistopy.interfaces

import us.huseli.fistopy.dataclasses.album.IAlbum
import us.huseli.fistopy.dataclasses.track.ITrackCombo

interface IExternalTrack : IStringIdItem {
    override val id: String
    val title: String

    fun toTrackCombo(isInLibrary: Boolean, album: IAlbum? = null): ITrackCombo
}
