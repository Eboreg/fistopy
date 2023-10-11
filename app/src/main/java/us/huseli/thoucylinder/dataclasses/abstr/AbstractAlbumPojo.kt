package us.huseli.thoucylinder.dataclasses.abstr

import us.huseli.thoucylinder.dataclasses.entities.Album
import us.huseli.thoucylinder.dataclasses.entities.Genre
import us.huseli.thoucylinder.dataclasses.entities.Style

abstract class AbstractAlbumPojo {
    abstract val album: Album
    abstract val genres: List<Genre>
    abstract val styles: List<Style>
}