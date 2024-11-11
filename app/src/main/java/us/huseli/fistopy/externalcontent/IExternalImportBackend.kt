package us.huseli.fistopy.externalcontent

import us.huseli.fistopy.externalcontent.holders.AbstractAlbumImportHolder

interface IExternalImportBackend {
    val albumImportHolder: AbstractAlbumImportHolder<*>
}
