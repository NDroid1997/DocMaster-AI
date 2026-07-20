package com.ndroid.docmasterai.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ndroid.docmasterai.ui.screens.home.HomeViewModel

data class NavBarItem(
    val route: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    NavBarItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavBarItem(Screen.Scanner, "Scan", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    NavBarItem(Screen.PdfTools, "Tools", Icons.Filled.Construction, Icons.Outlined.Construction),
    NavBarItem(Screen.FileManager, "Files", Icons.Filled.Folder, Icons.Outlined.FolderOpen),
    NavBarItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onScanClick:() -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val viewModel = hiltViewModel<HomeViewModel>()

    NavigationBar(
        modifier = modifier,
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hasRoute(item.route::class) == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if(item.route == Screen.Scanner){
                        onScanClick()
                        return@NavigationBarItem
                    }
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}
