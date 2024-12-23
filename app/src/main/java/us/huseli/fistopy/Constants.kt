package us.huseli.fistopy

object Constants {
    const val CUSTOM_USER_AGENT = "Fistopy/${BuildConfig.VERSION_NAME} ( https://github.com/Eboreg/fistopy )"
    const val IMAGE_THUMBNAIL_MAX_WIDTH_PX = 280
    const val IMAGE_THUMBNAIL_MIN_WIDTH_PX = 200
    const val LASTFM_AUTH_URL =
        "https://www.last.fm/api/auth/?api_key=${BuildConfig.lastFmApiKey}&cb=klaatu://${BuildConfig.hostName}/lastfm/auth"
    const val MAX_CONCURRENT_TRACK_DOWNLOADS = 3
    const val NAV_ARG_ALBUM = "album"
    const val NAV_ARG_ARTIST = "artist"
    const val NAV_ARG_PLAYLIST = "playlist"
    const val PREF_APP_START_COUNT = "appStartCount"
    const val PREF_AUTO_IMPORT_LOCAL_MUSIC = "autoImportLocalMusic"
    const val PREF_CURRENT_TRACK_POSITION = "currentTrackPosition"
    const val PREF_CURRENT_TUTORIAL_PAGE = "currentTutorialPage"
    const val PREF_LASTFM_SCROBBLE = "lastFmScrobble"
    const val PREF_LASTFM_SESSION_KEY = "lastFmSessionKey"
    const val PREF_LASTFM_USERNAME = "lastFmUsername"
    const val PREF_LIBRARY_RADIO_NOVELTY = "libraryRadioNovelty"
    const val PREF_LOCAL_MUSIC_URI = "localMusicUri"
    const val PREF_QUEUE_INDEX = "queueIndex"
    const val PREF_REGION = "region"
    const val PREF_SPOTIFY_CODE_VERIFIER = "spotifyCodeVerifier"
    const val PREF_SPOTIFY_OAUTH2_TOKEN_CC = "spotifyOAuth2TokenCC"
    const val PREF_SPOTIFY_OAUTH2_TOKEN_PKCE = "spotifyOAuth2Token"
    const val PREF_START_DESTINATION = "startDestination"
    const val PREF_UMLAUTIFY = "umlautify"
    const val PREF_WIDGET_BUTTONS = "widgetButtons"
}
