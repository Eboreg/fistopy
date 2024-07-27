package us.huseli.fistopy.externalcontent

import us.huseli.fistopy.externalcontent.holders.AbstractAlbumImportHolder
import us.huseli.fistopy.interfaces.IExternalAlbum

interface IExternalImportBackend<T : IExternalAlbum> {
    val albumImportHolder: AbstractAlbumImportHolder<T>
}
