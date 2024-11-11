package us.huseli.fistopy

import coil.intercept.Interceptor
import coil.map.Mapper
import coil.request.ImageResult
import coil.request.Options
import coil.size.Size
import coil.size.pxOrElse
import us.huseli.fistopy.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_PX
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.ILogger

abstract class AbstractThumbnailMapper<T : Any> : Mapper<T, String> {
    abstract fun getFullImageUrl(data: T): String?
    abstract fun getThumbnailUrl(data: T): String?

    private fun shouldGetFullImage(options: Options): Boolean {
        val height = options.size.height.pxOrElse { 0 }
        val width = options.size.width.pxOrElse { 0 }

        return height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
    }

    override fun map(data: T, options: Options): String? {
        return if (shouldGetFullImage(options)) getFullImageUrl(data) ?: getThumbnailUrl(data)
        else getThumbnailUrl(data) ?: getFullImageUrl(data)
    }
}


class AlbumArtMapper : AbstractThumbnailMapper<IAlbumArtOwner>() {
    override fun getFullImageUrl(data: IAlbumArtOwner): String? = data.fullImageUrl
    override fun getThumbnailUrl(data: IAlbumArtOwner): String? = data.thumbnailUrl
}


class MediaStoreImageMapper : AbstractThumbnailMapper<MediaStoreImage>() {
    override fun getFullImageUrl(data: MediaStoreImage): String = data.fullUriString
    override fun getThumbnailUrl(data: MediaStoreImage): String = data.thumbnailUriString
}


class ThumbnailInterceptor : Interceptor, ILogger {
    private fun getModel(data: Any, size: Size): Any {
        var url: String? = null

        if (data is MediaStoreImage) {
            url = if (shouldGetFullImage(size)) data.fullUriString else data.thumbnailUriString
        }
        if (data is IAlbumArtOwner) {
            url =
                if (shouldGetFullImage(size)) data.fullImageUrl ?: data.thumbnailUrl
                else data.thumbnailUrl ?: data.fullImageUrl
        }
        if (data is IHasMusicBrainzIds && url == null) {
            val px = maxOf(
                size.height.pxOrElse { 250 }.roundToClosest(listOf(250, 500, 1200)),
                size.width.pxOrElse { 250 }.roundToClosest(listOf(250, 500, 1200)),
            )

            url = data.musicBrainzReleaseId?.let { "https://coverartarchive.org/release/$it/front-$px" }
                ?: data.musicBrainzReleaseGroupId?.let { "https://coverartarchive.org/release-group/$it/front-$px" }
        }

        return url ?: data
    }

    private fun shouldGetFullImage(size: Size): Boolean {
        val height = size.height.pxOrElse { 0 }
        val width = size.width.pxOrElse { 0 }

        return height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
    }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val model = getModel(chain.request.data, chain.size)

        log("Coil", "ThumbnailInterceptor: trying $model")
        return chain.proceed(chain.request.newBuilder(chain.request.context).data(model).build())
    }
}
