package us.huseli.fistopy

import androidx.navigation.NavType
import androidx.navigation.navArgument
import us.huseli.fistopy.Constants.NAV_ARG_ALBUM
import us.huseli.fistopy.Constants.NAV_ARG_ARTIST
import us.huseli.fistopy.Constants.NAV_ARG_PLAYLIST
import us.huseli.fistopy.compose.MenuItemId
import us.huseli.retaintheme.navigation.AbstractDestination
import us.huseli.retaintheme.navigation.AbstractSimpleDestination

abstract class Destination(override val menuItemId: MenuItemId) :
    AbstractSimpleDestination<MenuItemId>(menuItemId.route, menuItemId)

object SearchDestination : Destination(MenuItemId.SEARCH)

object LibraryDestination : Destination(MenuItemId.LIBRARY)

object QueueDestination : Destination(MenuItemId.QUEUE)

object DebugDestination : Destination(MenuItemId.DEBUG)

object ImportDestination : Destination(MenuItemId.IMPORT)

object DownloadsDestination : Destination(MenuItemId.DOWNLOADS)

object SettingsDestination : Destination(MenuItemId.SETTINGS)

object AlbumDestination : AbstractDestination<MenuItemId>() {
    override val routeTemplate = "album/{$NAV_ARG_ALBUM}"
    override val arguments = listOf(navArgument(NAV_ARG_ALBUM) { type = NavType.StringType })
    override val menuItemId: MenuItemId? = null

    fun route(albumId: String) = "album/$albumId"
}

object ArtistDestination : AbstractDestination<MenuItemId>() {
    override val arguments = listOf(navArgument(NAV_ARG_ARTIST) { type = NavType.StringType })
    override val routeTemplate = "artist/{$NAV_ARG_ARTIST}"
    override val menuItemId: MenuItemId? = null

    fun route(artistId: String) = "artist/$artistId"
}

object PlaylistDestination : AbstractDestination<MenuItemId>() {
    override val routeTemplate = "playlist/{$NAV_ARG_PLAYLIST}"
    override val arguments = listOf(navArgument(NAV_ARG_PLAYLIST) { type = NavType.StringType })
    override val menuItemId: MenuItemId? = null

    fun route(playlistId: String) = "playlist/$playlistId"
}

object TutorialDestination : AbstractSimpleDestination<MenuItemId>("tutorial")
