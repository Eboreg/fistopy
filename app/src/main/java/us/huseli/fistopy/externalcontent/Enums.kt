package us.huseli.fistopy.externalcontent

import androidx.annotation.StringRes
import us.huseli.fistopy.R

enum class ImportBackend(@StringRes val stringRes: Int) {
    LOCAL(R.string.local),
    LAST_FM(R.string.last_fm),
    SPOTIFY(R.string.spotify),
}

enum class SearchBackend(@StringRes val stringRes: Int) {
    YOUTUBE(R.string.youtube),
    MUSICBRAINZ(R.string.musicbrainz),
    SPOTIFY(R.string.spotify),
}

enum class ExternalListType(@StringRes val stringRes: Int) {
    ALBUMS(R.string.albums),
    TRACKS(R.string.tracks),
}

enum class SearchCapability {
    ALBUM,
    ARTIST,
    FREE_TEXT,
    TRACK,
}
