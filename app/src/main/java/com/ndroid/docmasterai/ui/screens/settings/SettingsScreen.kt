package com.ndroid.docmasterai.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Premium status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (uiState.isPremium) listOf(Success, CardGradient4End)
                                else listOf(GradientStart, GradientMid),
                                start = Offset(0f, 0f),
                                end = Offset(600f, 200f),
                            ),
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (uiState.isPremium) Icons.Filled.Verified else Icons.Filled.WorkspacePremium,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (uiState.isPremium) "Premium Active ✨" else "Free Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                if (uiState.isPremium) "All features unlocked"
                                else "Upgrade for unlimited access",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        if (!uiState.isPremium) {
                            FilledTonalButton(
                                onClick = { viewModel.upgradePremium() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("Upgrade")
                            }
                        }
                    }
                }
            }

            // Appearance
            SettingsSection("Appearance") {
                SettingsItem(
                    icon = Icons.Filled.DarkMode,
                    title = "Theme",
                    subtitle = uiState.darkMode.replaceFirstChar { it.uppercase() },
                    onClick = { viewModel.cycleDarkMode() },
                )
            }

            // Defaults
            SettingsSection("Defaults") {
                SettingsItem(
                    icon = Icons.Filled.SaveAlt,
                    title = "Default Save Format",
                    subtitle = uiState.defaultSaveFormat.uppercase(),
                    onClick = { viewModel.cycleDefaultFormat() },
                )
                SettingsItem(
                    icon = Icons.Filled.HighQuality,
                    title = "Scan Quality",
                    subtitle = "${uiState.scanQuality}%",
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Filled.Language,
                    title = "OCR Language",
                    subtitle = uiState.ocrLanguage.replaceFirstChar { it.uppercase() },
                    onClick = { },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Auto Enhance Scans", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = uiState.autoEnhance,
                        onCheckedChange = { viewModel.toggleAutoEnhance() },
                        colors = SwitchDefaults.colors(checkedThumbColor = GradientStart),
                    )
                }
            }

            // Storage
            SettingsSection("Storage") {
                SettingsItem(
                    icon = Icons.Filled.Storage,
                    title = "Storage Used",
                    subtitle = formatSize(uiState.storageUsed),
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Filled.CleaningServices,
                    title = "Clear Cache",
                    subtitle = "Free up space",
                    onClick = { viewModel.clearCache() },
                )
            }

            // About
            SettingsSection("About") {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "",
                    onClick = { },
                )
                SettingsItem(
                    icon = Icons.Filled.Star,
                    title = "Rate App",
                    subtitle = "",
                    onClick = { },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
