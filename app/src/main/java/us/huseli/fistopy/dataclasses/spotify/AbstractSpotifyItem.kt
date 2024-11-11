package us.huseli.fistopy.dataclasses.spotify

import us.huseli.fistopy.interfaces.IStringIdItem

abstract class AbstractSpotifyItem : IStringIdItem {
    abstract override val id: String

    override fun equals(other: Any?) = other?.javaClass == javaClass && (other as AbstractSpotifyItem).id == id
    override fun hashCode() = id.hashCode()
}
