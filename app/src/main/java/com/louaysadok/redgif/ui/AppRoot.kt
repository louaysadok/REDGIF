@file:OptIn(ExperimentalMaterial3Api::class)

package com.louaysadok.redgif.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.louaysadok.redgif.AppContainer
import com.louaysadok.redgif.data.media.ViewerQueue
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.data.net.Order
import com.louaysadok.redgif.data.settings.AppSettings
import com.louaysadok.redgif.ui.account.AccountScreen
import com.louaysadok.redgif.ui.account.LoginScreen
import com.louaysadok.redgif.ui.common.AgeGateDialog
import com.louaysadok.redgif.ui.favorites.FavoritesScreen
import com.louaysadok.redgif.ui.feed.FeedKind
import com.louaysadok.redgif.ui.feed.FeedScreen
import com.louaysadok.redgif.ui.feed.FeedSource
import com.louaysadok.redgif.ui.player.ViewerScreen
import com.louaysadok.redgif.ui.search.SearchScreen
import com.louaysadok.redgif.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val ACCOUNT = "account"
    const val SETTINGS = "settings"
    const val LOGIN = "login"
    const val RESULTS = "results/{query}"
    const val CREATOR = "creator/{name}"
    const val VIEWER = "viewer/{index}"

    fun results(query: String) = "results/${Uri.encode(query)}"
    fun creator(name: String) = "creator/${Uri.encode(name)}"
    fun viewer(index: Int) = "viewer/$index"
}

@Composable
fun AppRoot(container: AppContainer, onExit: () -> Unit) {
    val settingsState by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings: AppSettings = settingsState ?: return

    val scope = rememberCoroutineScope()
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun openDrawer() {
        scope.launch { drawer.open() }
    }

    fun navigateTop(route: String) {
        scope.launch { drawer.close() }
        nav.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun openViewer(list: List<RedMedia>, index: Int) {
        ViewerQueue.set(list)
        nav.navigate(Routes.viewer(index))
    }

    fun openCreator(name: String) {
        nav.navigate(Routes.creator(name))
    }

    if (!settings.adultConfirmed) {
        AgeGateDialog(
            onConfirm = { scope.launch { container.settings.update { it.copy(adultConfirmed = true) } } },
            onExit = onExit,
        )
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text("RedGIF", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (settings.signedIn) "Signed in to RedGIFs" else "Browsing as guest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
                DrawerItem(Icons.Filled.Home, "Browse", Routes.HOME, currentRoute, ::navigateTop)
                DrawerItem(Icons.Filled.Search, "Search", Routes.SEARCH, currentRoute, ::navigateTop)
                DrawerItem(Icons.Filled.Bookmark, "Favorites", Routes.FAVORITES, currentRoute, ::navigateTop)
                DrawerItem(Icons.Filled.Person, "Account", Routes.ACCOUNT, currentRoute, ::navigateTop)
                DrawerItem(Icons.Filled.Settings, "Settings", Routes.SETTINGS, currentRoute, ::navigateTop)
            }
        },
    ) {
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                FeedScreen(
                    base = FeedSource(FeedKind.BROWSE),
                    title = "RedGIF",
                    onMenu = ::openDrawer,
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenViewer = ::openViewer,
                    onOpenCreator = ::openCreator,
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onSearch = { query -> nav.navigate(Routes.results(query)) },
                    onOpenCreator = ::openCreator,
                )
            }

            composable(
                Routes.RESULTS,
                arguments = listOf(navArgument("query") { type = NavType.StringType }),
            ) { entry ->
                val query = entry.arguments?.getString("query").orEmpty()
                FeedScreen(
                    base = FeedSource(FeedKind.SEARCH, query = query),
                    title = query,
                    onBack = { nav.popBackStack() },
                    onOpenViewer = ::openViewer,
                    onOpenCreator = ::openCreator,
                )
            }

            composable(
                Routes.CREATOR,
                arguments = listOf(navArgument("name") { type = NavType.StringType }),
            ) { entry ->
                val name = entry.arguments?.getString("name").orEmpty()
                FeedScreen(
                    base = FeedSource(FeedKind.CREATOR, order = Order.LATEST, creator = name),
                    title = "@$name",
                    onBack = { nav.popBackStack() },
                    onOpenViewer = ::openViewer,
                    onOpenCreator = ::openCreator,
                )
            }

            composable(
                Routes.VIEWER,
                arguments = listOf(navArgument("index") { type = NavType.IntType }),
            ) { entry ->
                ViewerScreen(
                    startIndex = entry.arguments?.getInt("index") ?: 0,
                    onBack = { nav.popBackStack() },
                    onOpenCreator = ::openCreator,
                )
            }

            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onMenu = ::openDrawer,
                    onOpenViewer = ::openViewer,
                    onOpenCreator = ::openCreator,
                )
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onMenu = ::openDrawer,
                    onLogin = { nav.navigate(Routes.LOGIN) },
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onMenu = ::openDrawer)
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    route: String,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = currentRoute == route,
        onClick = { onNavigate(route) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
