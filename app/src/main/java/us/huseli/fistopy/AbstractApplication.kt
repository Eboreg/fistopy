package us.huseli.fistopy

import coil.ImageLoader
import coil.ImageLoaderFactory

abstract class AbstractApplication : android.app.Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AlbumArtMapper())
                add(MediaStoreImageMapper())
                add(CoverArtArchiveInterceptor())
            }
            .respectCacheHeaders(false)
            .build()
    }
}
