package com.ndroid.docmasterai.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ndroid.docmasterai.ui.screens.converter.ConverterScreen
import com.ndroid.docmasterai.ui.screens.filemanager.FileManagerScreen
import com.ndroid.docmasterai.ui.screens.home.HomeScreen
import com.ndroid.docmasterai.ui.screens.ocr.OcrScreen
import com.ndroid.docmasterai.ui.screens.onboarding.OnboardingScreen
import com.ndroid.docmasterai.ui.screens.pdftools.PdfToolsScreen
import com.ndroid.docmasterai.ui.screens.resize.ResizeScreen
import com.ndroid.docmasterai.ui.screens.scanner.ScanResultScreen
import com.ndroid.docmasterai.ui.screens.scanner.ScannerScreen
import com.ndroid.docmasterai.ui.screens.settings.SettingsScreen
import com.ndroid.docmasterai.ui.screens.splash.SplashScreen

@Composable
fun DocMasterNavHost(
    navController: NavHostController,
    startDestination: Screen = Screen.Splash,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { 100 }) },
        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { -100 }) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(initialOffsetX = { -100 }) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(targetOffsetX = { 100 }) },
    ) {
        composable<Screen.Splash> {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Onboarding> {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Home> {
            HomeScreen(
                onNavigateToScanner = { navController.navigate(Screen.Scanner) },
                onNavigateToOcr = { navController.navigate(Screen.Ocr) },
                onNavigateToConverter = { navController.navigate(Screen.Converter) },
                onNavigateToPdfTools = { navController.navigate(Screen.PdfTools) },
                onNavigateToResize = { navController.navigate(Screen.Resize) },
                onNavigateToFileManager = { navController.navigate(Screen.FileManager) },
            )
        }

        composable<Screen.Scanner> {
            ScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { docId ->
                    navController.navigate(Screen.ScanResult(docId))
                }
            )
        }

        composable<Screen.ScanResult> { backStackEntry ->
            val scanResult = backStackEntry.toRoute<Screen.ScanResult>()
            ScanResultScreen(
                documentId = scanResult.documentId,
                onNavigateBack = { navController.popBackStack() },
                onSaveComplete = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Ocr> {
            OcrScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.Converter> {
            ConverterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.PdfTools> {
            PdfToolsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.Resize> {
            ResizeScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.FileManager> {
            FileManagerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.Settings> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
