package us.huseli.fistopy.compose.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import us.huseli.fistopy.Logger
import us.huseli.fistopy.dataclasses.MediaStoreImage
import us.huseli.fistopy.interfaces.IAlbumArtOwner
import us.huseli.fistopy.interfaces.IHasMusicBrainzIds

private fun getThumbnailModels(data: Any?): List<Any> {
    val result = mutableListOf<Any>()

    if (data is MediaStoreImage) result.add(data)
    if (data is IAlbumArtOwner && (data.fullImageUrl != null || data.thumbnailUrl != null)) result.add(
        object : IAlbumArtOwner {
            override val fullImageUrl: String? = data.fullImageUrl
            override val thumbnailUrl: String? = data.thumbnailUrl
            override fun toString() = "IAlbumArtOwner<fullImageUrl=$fullImageUrl, thumbnailUrl=$thumbnailUrl>"
        }
    )
    if (data is IHasMusicBrainzIds) {
        if (data.musicBrainzReleaseId != null) result.add(
            object : IHasMusicBrainzIds {
                override val musicBrainzReleaseGroupId: String? = null
                override val musicBrainzReleaseId: String? = data.musicBrainzReleaseId
                override fun toString() =
                    "IHasMusicBrainzIds<musicBrainzReleaseGroupId=$musicBrainzReleaseGroupId, musicBrainzReleaseId=$musicBrainzReleaseId>"
            }
        )
        if (data.musicBrainzReleaseGroupId != null) result.add(
            object : IHasMusicBrainzIds {
                override val musicBrainzReleaseGroupId: String? = data.musicBrainzReleaseGroupId
                override val musicBrainzReleaseId: String? = null
                override fun toString() =
                    "IHasMusicBrainzIds<musicBrainzReleaseGroupId=$musicBrainzReleaseGroupId, musicBrainzReleaseId=$musicBrainzReleaseId>"
            }
        )
    }
    if (data != null) result.add(data)

    return result.toList()
}

@Composable
fun ThumbnailImage(
    model: Any?,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    val context = LocalContext.current
    var loadFailed by remember(model) { mutableStateOf(false) }
    val models = remember(model) { getThumbnailModels(model) }
    var modelIdx by remember(models) { mutableIntStateOf(0) }
    val request = remember(models, modelIdx) {
        with(ImageRequest.Builder(context).data(models.getOrNull(modelIdx))) {
            if (customModelSize != null) size(customModelSize)
            build()
        }
    }

    if (!loadFailed) {
        Logger.log("Coil", "ThumbnailImage: request.data=${request.data}, modelIdx=$modelIdx, models=$models")

        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = {
                if (modelIdx < models.lastIndex) modelIdx++
                else loadFailed = true
            },
            modifier = modifier,
        )
    } else if (placeholderIcon != null) {
        Icon(
            imageVector = placeholderIcon,
            contentDescription = null,
            modifier = modifier.padding(10.dp).fillMaxSize(),
            tint = placeholderIconTint ?: LocalContentColor.current,
        )
    }
}

@Composable
fun Thumbnail(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    borderWidth: Dp? = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    Surface(
        shape = shape,
        modifier = modifier.aspectRatio(1f).fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = borderWidth?.let { BorderStroke(it, borderColor) },
        content = {
            ThumbnailImage(
                model = model,
                placeholderIcon = placeholderIcon,
                placeholderIconTint = placeholderIconTint,
                customModelSize = customModelSize,
            )
        },
    )
}

@Composable
fun Thumbnail4x4(
    models: List<Any?>,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    borderWidth: Dp? = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    placeholderIcon: ImageVector? = null,
    placeholderIconTint: Color? = null,
    customModelSize: Int? = null,
) {
    if (models.size < 4) {
        Thumbnail(
            model = models.firstOrNull(),
            modifier = modifier,
            shape = shape,
            borderWidth = borderWidth,
            borderColor = borderColor,
            placeholderIcon = placeholderIcon,
            placeholderIconTint = placeholderIconTint,
            customModelSize = customModelSize,
        )
    } else {
        Surface(
            shape = shape,
            modifier = modifier.aspectRatio(1f).fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = borderWidth?.let { BorderStroke(it, borderColor) },
        ) {
            Column {
                for (chunk in models.subList(0, 4).chunked(2)) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (model in chunk) {
                            ThumbnailImage(
                                model = model,
                                placeholderIcon = placeholderIcon,
                                placeholderIconTint = placeholderIconTint,
                                modifier = Modifier.weight(1f),
                                customModelSize = customModelSize?.div(4),
                            )
                        }
                    }
                }
            }
        }
    }
}
