package com.tricosoft.booknest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.components.ToastHost
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.screens.AuthScreen
import com.tricosoft.booknest.ui.screens.BookDetailScreen
import com.tricosoft.booknest.ui.screens.EditorScreen
import com.tricosoft.booknest.ui.screens.ExploreScreen
import com.tricosoft.booknest.ui.screens.HomeScreen
import com.tricosoft.booknest.ui.screens.ProfileScreen
import com.tricosoft.booknest.ui.screens.ReaderScreen
import com.tricosoft.booknest.ui.screens.SettingsScreen
import com.tricosoft.booknest.ui.screens.WriteScreen

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("home", "Home", Icons.Outlined.Home),
    NavItem("explore", "Explore", Icons.Outlined.Search),
    NavItem("write", "Write", Icons.Outlined.Edit),
    NavItem("profile", "Profile", Icons.Outlined.Person),
)

@Composable
fun BookNestRoot() {
    val g = LocalGraph.current
    val c = bn()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    // Web er moto: read page e bottom nav nei (auth page e-o lukai)
    val showBottom = route != null && route != "auth" && !route.startsWith("read/")
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .imePadding()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier
                    .widthIn(max = 512.dp)
                    .fillMaxSize()) {
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") { HomeScreen(nav) }
                        composable("explore") { ExploreScreen(nav) }
                        composable("write") { WriteScreen(nav) }
                        composable("profile") { ProfileScreen(nav) }
                        composable("settings") { SettingsScreen(nav) }
                        composable("auth") { AuthScreen(nav) }
                        composable(
                            "book/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { e -> BookDetailScreen(nav, e.arguments?.getString("id") ?: "") }
                        composable(
                            "read/{id}?chapter={chapter}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType },
                                navArgument("chapter") { type = NavType.IntType; defaultValue = 0 },
                            ),
                        ) { e ->
                            ReaderScreen(nav, e.arguments?.getString("id") ?: "", e.arguments?.getInt("chapter") ?: 0)
                        }
                        composable(
                            "editor/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { e -> EditorScreen(nav, e.arguments?.getString("id") ?: "new") }
                    }
                }
            }
            if (showBottom && !imeVisible) {
                BottomBar(route = route, onSelect = { target -> navigateTab(nav, target) })
            }
        }

        ToastHost(
            toaster = g.toaster,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
    }
}

private fun navigateTab(nav: NavController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(route: String?, onSelect: (String) -> Unit) {
    val c = bn()
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(c.background.copy(alpha = 0.95f))) {
        HorizontalDivider(color = c.border, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NAV_ITEMS.forEach { item ->
                val active = route == item.route
                val tint = if (active) c.primary else c.mutedForeground
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(item.route) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Txt(item.label, size = 10, weight = FontWeight.Medium, color = tint)
                }
            }
        }
    }
}
