package us.huseli.fistopy.dataclasses.spotify

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import us.huseli.fistopy.R
import us.huseli.fistopy.enums.AlbumType

enum class SpotifyAlbumType(@StringRes val stringRes: Int) {
    @SerializedName("album")
    ALBUM(R.string.album),

    @SerializedName("single")
    SINGLE(R.string.single),

    @SerializedName("compilation")
    COMPILATION(R.string.compilation);

    val nativeAlbumType: AlbumType
        get() = when (this) {
            ALBUM -> AlbumType.ALBUM
            SINGLE -> AlbumType.SINGLE
            COMPILATION -> AlbumType.COMPILATION
        }
}