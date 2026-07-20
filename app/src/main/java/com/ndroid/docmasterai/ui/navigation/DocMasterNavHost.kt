package com.ndroid.docmasterai.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ndroid.docmasterai.ui.screens.converter.ConversionResultScreen
import com.ndroid.docmasterai.ui.screens.converter.ConverterScreen
import com.ndroid.docmasterai.ui.screens.filemanager.FileManagerScreen
import com.ndroid.docmasterai.ui.screens.home.HomeScreen
import com.ndroid.docmasterai.ui.screens.auth.LoginScreen
import com.ndroid.docmasterai.ui.screens.ocr.OcrScreen
import com.ndroid.docmasterai.ui.screens.onboarding.OnboardingScreen
import com.ndroid.docmasterai.ui.screens.pdftools.PdfPageSelectorScreen
import com.ndroid.docmasterai.ui.screens.pdftools.PdfPasswordScreen
import com.ndroid.docmasterai.ui.screens.pdftools.PdfToolsScreen
import com.ndroid.docmasterai.ui.screens.pdftools.PdfWatermarkScreen
import com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder.PdfPageReorderScreen
import com.ndroid.docmasterai.ui.screens.premium.PremiumUpgradeScreen
import com.ndroid.docmasterai.ui.screens.resize.ResizeScreen
import com.ndroid.docmasterai.ui.screens.scanner.ScanResultScreen
import com.ndroid.docmasterai.ui.screens.scanner.ScannerScreen
import com.ndroid.docmasterai.ui.screens.settings.SettingsScreen
import com.ndroid.docmasterai.ui.screens.splash.SplashScreen

@Composable
fun DocMasterNavHost(
    navController: NavHostController,
    onStartScan: () -> Unit,
    onItemClick:(documentId: String, images: List<Uri>, pdf: Uri?) -> Unit,
    startDestination: Screen = Screen.Splash,
    modifier: Modifier = Modifier,
) {
    val preferences = hiltViewModel<com.ndroid.docmasterai.ui.screens.home.HomeViewModel>().premiumManager.preferences // Accessible via premiumManager in Hilt

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
                onNavigateToLogin = {
                    navController.navigate(Screen.Login) {
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
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                preferences = hiltViewModel<com.ndroid.docmasterai.ui.screens.home.HomeViewModel>().premiumManager.preferences
            )
        }

        composable<Screen.Home> {
            HomeScreen(
                onNavigateToScanner = onStartScan,
                onNavigateToOcr = { navController.navigate(Screen.Ocr) },
                onNavigateToConverter = { navController.navigate(Screen.Converter) },
                onNavigateToPdfTools = { navController.navigate(Screen.PdfTools) },
                onNavigateToResize = { navController.navigate(Screen.Resize) },
                onNavigateToFileManager = { navController.navigate(Screen.FileManager) },
                onNavigateToPremium = { navController.navigate(Screen.PremiumUpgrade) },
                onDocItemClick = { documentId, images, pdf ->
                    onItemClick(documentId, images, pdf)
                },
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
            ConverterScreen(
                onNavigateBack = { navController.popBackStack() },
                onConversionComplete = { resultPath ->
                    navController.navigate(Screen.ConversionResult(resultPath))
                },
                onNavigateToPremium = { navController.navigate(Screen.PremiumUpgrade) }
            )
        }

        composable<Screen.ConversionResult> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.ConversionResult>()
            ConversionResultScreen(
                resultPath = args.resultPath,
                onNavigateBack = {
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
        }

        composable<Screen.PdfTools> {
            PdfToolsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSplit = { uri, name ->
                    navController.navigate(Screen.PdfPageSelector(uri.toString(), name))
                },
                onNavigateToReorder = { uri, name ->
                    navController.navigate(Screen.PdfPageReorder(uri.toString(), name))
                },
                onNavigateToWatermark = { uri, name ->
                    navController.navigate(Screen.PdfWatermark(uri.toString(), name))
                },
                onNavigateToPassword = { uri, name, isProtect ->
                    navController.navigate(Screen.PdfPassword(uri.toString(), name, isProtect))
                },
                onNavigateToPremium = { navController.navigate(Screen.PremiumUpgrade) }
            )
        }

        composable<Screen.PdfPageSelector> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PdfPageSelector>()
            PdfPageSelectorScreen(
                initialUri = Uri.parse(args.uri),
                initialFileName = args.fileName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.PdfPageReorder> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PdfPageReorder>()
            PdfPageReorderScreen(
                initialUri = Uri.parse(args.uri),
                initialFileName = args.fileName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.PdfWatermark> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PdfWatermark>()
            PdfWatermarkScreen(
                uri = Uri.parse(args.uri),
                fileName = args.fileName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.PdfPassword> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PdfPassword>()
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.PdfTools)
            }
            PdfPasswordScreen(
                uri = Uri.parse(args.uri),
                fileName = args.fileName,
                isProtect = args.isProtect,
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel(parentEntry)
            )
        }

        composable<Screen.Resize> {
            ResizeScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.FileManager> {
            FileManagerScreen(onNavigateBack = { navController.popBackStack() }, onItemClick = onItemClick)
        }

        composable<Screen.Settings> {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable<Screen.PremiumUpgrade> {
            val premiumManager = hiltViewModel<com.ndroid.docmasterai.ui.screens.home.HomeViewModel>().premiumManager
            PremiumUpgradeScreen(
                onNavigateBack = { navController.popBackStack() },
                premiumManager = premiumManager
            )
        }
    }
}
