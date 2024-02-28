package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import us.huseli.retaintheme.compose.ResponsiveScaffold
import us.huseli.retaintheme.compose.SnackbarHosts
import us.huseli.thoucylinder.AddDestination
import us.huseli.thoucylinder.BuildConfig
import us.huseli.thoucylinder.DebugDestination
import us.huseli.thoucylinder.DownloadsDestination
import us.huseli.thoucylinder.ImportDestination
import us.huseli.thoucylinder.LibraryDestination
import us.huseli.thoucylinder.QueueDestination
import us.huseli.thoucylinder.RecommendationsDestination
import us.huseli.thoucylinder.SettingsDestination
import us.huseli.thoucylinder.umlautify

@Composable
fun ThouCylinderScaffold(
    modifier: Modifier = Modifier,
    activeMenuItemId: MenuItemId?,
    onNavigate: (route: String) -> Unit,
    onInnerPaddingChange: (PaddingValues) -> Unit,
    onContentAreaSizeChange: (DpSize) -> Unit,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    var isCoverExpanded by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onMenuItemClick = { menuItem: MenuItemId ->
        when (menuItem) {
            MenuItemId.SEARCH_YOUTUBE -> onNavigate(AddDestination.route)
            MenuItemId.LIBRARY -> onNavigate(LibraryDestination.route)
            MenuItemId.QUEUE -> onNavigate(QueueDestination.route)
            MenuItemId.IMPORT -> onNavigate(ImportDestination.route)
            MenuItemId.DEBUG -> onNavigate(DebugDestination.route)
            MenuItemId.DOWNLOADS -> onNavigate(DownloadsDestination.route)
            MenuItemId.SETTINGS -> onNavigate(SettingsDestination.route)
            MenuItemId.MENU -> scope.launch { drawerState.open() }
            MenuItemId.RECOMMENDATIONS -> onNavigate(RecommendationsDestination.route)
        }
        isCoverExpanded = false
    }
    val menuItems = getMenuItems().filter { !it.debugOnly || BuildConfig.DEBUG }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape,
                modifier = Modifier.width(IntrinsicSize.Min),
            ) {
                menuItems.filter { it.showInDrawer }.forEach { item ->
                    NavigationDrawerItem(
                        shape = RectangleShape,
                        label = { item.description?.also { Text(it.umlautify()) } },
                        selected = activeMenuItemId == item.id,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onMenuItemClick(item.id)
                        },
                        icon = item.icon,
                    )
                }
            }
        }
    ) {
        ResponsiveScaffold(
            portraitMenuModifier = Modifier.height(80.dp),
            activeMenuItemId = activeMenuItemId,
            menuItems = menuItems.filter { it.showInMainMenu },
            onMenuItemClick = onMenuItemClick,
            landscapeMenu = { innerPadding ->
                NavigationRail(modifier = Modifier.padding(innerPadding)) {
                    menuItems.filter { it.showInMainMenu }.forEach { item ->
                        NavigationRailItem(
                            selected = activeMenuItemId == item.id,
                            onClick = { onMenuItemClick(item.id) },
                            icon = item.icon,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHosts() },
        ) { innerPadding ->
            LaunchedEffect(innerPadding) {
                onInnerPaddingChange(innerPadding)
            }

            BoxWithConstraints(modifier = modifier.fillMaxSize().padding(innerPadding)) {
                val contentAreaSize by remember(this.maxWidth, this.maxHeight) {
                    mutableStateOf(DpSize(this.maxWidth, this.maxHeight))
                }

                LaunchedEffect(contentAreaSize) {
                    onContentAreaSizeChange(contentAreaSize)
                }

                content()
            }
        }
    }
}
