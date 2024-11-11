package us.huseli.fistopy.dataclasses.musicbrainz

import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractMusicBrainzItem : IStringIdItem {
    abstract override val id: String

    override fun equals(other: Any?) = other?.javaClass == javaClass && (other as AbstractMusicBrainzItem).id == id
    override fun hashCode() = id.hashCode()
}
