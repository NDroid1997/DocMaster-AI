package com.ndroid.docmasterai.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface Screen {
    @Serializable data object Splash : Screen
    @Serializable data object Onboarding : Screen
    @Serializable data object Login : Screen
    @Serializable data object Home : Screen
    @Serializable data object Scanner : Screen
    @Serializable data class ScanResult(val documentId: String = "") : Screen
    @Serializable data object Ocr : Screen
    @Serializable data object Converter : Screen
    @Serializable data object PdfTools : Screen
    @Serializable data object Resize : Screen
    @Serializable data object FileManager : Screen
    @Serializable data object Settings : Screen
    @Serializable data object PremiumUpgrade : Screen
    @Serializable data class ConversionResult(val resultPath: String) : Screen
    @Serializable data class PdfPageSelector(val uri: String, val fileName: String) : Screen
    @Serializable data class PdfPageReorder(val uri: String, val fileName: String) : Screen
    @Serializable data class PdfWatermark(val uri: String, val fileName: String) : Screen
    @Serializable data class PdfPassword(val uri: String, val fileName: String, val isProtect: Boolean) : Screen
}

enum class BottomNavItem(
    val route: Screen,
    val title: String,
    val iconName: String,
) {
    HOME(Screen.Home, "Home", "home"),
    SCANNER(Screen.Scanner, "Scan", "document_scanner"),
    TOOLS(Screen.PdfTools, "Tools", "build"),
    FILES(Screen.FileManager, "Files", "folder"),
    SETTINGS(Screen.Settings, "Settings", "settings"),
}
