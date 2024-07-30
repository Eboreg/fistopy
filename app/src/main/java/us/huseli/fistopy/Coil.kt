package us.huseli.fistopy

import coil.intercept.Interceptor
import coil.map.Mapper
import coil.request.ImageResult
import coil.request.Options
import coil.size.pxOrElse
import us.huseli.fistopy.Constants.IMAGE_THUMBNAIL_MAX_WIDTH_PX
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds
import us.huseli.fistopy.interfaces.ILogger

abstract class AbstractThumbnailMapper<T : Any> : Mapper<T, String> {
    fun shouldGetFullImage(options: Options): Boolean {
        val height = options.size.height.pxOrElse { 0 }
        val width = options.size.width.pxOrElse { 0 }

        return height > IMAGE_THUMBNAIL_MAX_WIDTH_PX || width > IMAGE_THUMBNAIL_MAX_WIDTH_PX
    }
}


class AlbumArtMapper : AbstractThumbnailMapper<IAlbumArtOwner>() {
    override fun map(data: IAlbumArtOwner, options: Options): String? =
        if (shouldGetFullImage(options)) data.fullImageUrl ?: data.thumbnailUrl
        else data.thumbnailUrl ?: data.fullImageUrl
}


class MediaStoreImageMapper : AbstractThumbnailMapper<MediaStoreImage>() {
    override fun map(data: MediaStoreImage, options: Options): String =
        if (shouldGetFullImage(options)) data.fullUriString
        else data.thumbnailUriString
}


class CoverArtArchiveInterceptor : Interceptor, ILogger {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        val size = maxOf(
            chain.size.height.pxOrElse { 250 }.roundToClosest(listOf(250, 500, 1200)),
            chain.size.width.pxOrElse { 250 }.roundToClosest(listOf(250, 500, 1200)),
        )

        if (data is IHasMusicBrainzIds) {
            val coverArtUrls = listOfNotNull(
                data.musicBrainzReleaseId?.let { "https://coverartarchive.org/release/$it/front-$size" },
                data.musicBrainzReleaseGroupId?.let { "https://coverartarchive.org/release-group/$it/front-$size" },
            )

            for (url in coverArtUrls) {
                try {
                    return chain.proceed(chain.request.newBuilder(chain.request.context).data(url).build())
                } catch (e: Exception) {
                    logError(e)
                }
            }
        }

        return chain.proceed(chain.request)
    }
}
