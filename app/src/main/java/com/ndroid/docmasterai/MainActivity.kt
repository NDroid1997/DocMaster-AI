package com.ndroid.docmasterai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ndroid.docmasterai.ui.navigation.BottomNavBar
import com.ndroid.docmasterai.ui.navigation.DocMasterNavHost
import com.ndroid.docmasterai.ui.navigation.Screen
import com.ndroid.docmasterai.ui.theme.DocMasterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocMasterTheme {
                DocMasterMainScreen()
            }
        }
    }
}

@Composable
fun DocMasterMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom bar only on main screens
    val showBottomBar = currentDestination?.let { dest ->
        listOf(
            Screen.Home::class,
            Screen.Scanner::class,
            Screen.PdfTools::class,
            Screen.FileManager::class,
            Screen.Settings::class,
        ).any { dest.hasRoute(it) }
    } ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        DocMasterNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
