package com.ndroid.docmasterai.ui.screens.converter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.ndroid.docmasterai.data.model.ConversionType
import com.ndroid.docmasterai.ui.theme.*

data class ConversionTool(
    val type: ConversionType,
    val icon: ImageVector,
    val gradientColors: List<Color>,
)

val conversionTools = listOf(
    ConversionTool(ConversionType.PDF_TO_IMAGE, Icons.Filled.Image, listOf(CardGradient1Start, CardGradient1End)),
    ConversionTool(ConversionType.IMAGE_TO_PDF, Icons.Filled.PictureAsPdf, listOf(CardGradient2Start, CardGradient2End)),
    ConversionTool(ConversionType.PNG_TO_JPG, Icons.Filled.SwapHoriz, listOf(CardGradient3Start, CardGradient3End)),
    ConversionTool(ConversionType.JPG_TO_PNG, Icons.Filled.SwapHoriz, listOf(CardGradient4Start, CardGradient4End)),
    ConversionTool(ConversionType.WEBP_TO_JPG, Icons.Filled.Transform, listOf(CardGradient5Start, CardGradient5End)),
    ConversionTool(ConversionType.PDF_TO_TEXT, Icons.Filled.TextFields, listOf(CardGradient1End, CardGradient2Start)),
    ConversionTool(ConversionType.IMAGE_TO_TEXT, Icons.Filled.TextSnippet, listOf(CardGradient2End, CardGradient3Start)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConverterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.startConversion(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Converter", fontWeight = FontWeight.SemiBold) },
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
                .padding(16.dp),
        ) {
            Text(
                "Select Conversion Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(conversionTools) { tool ->
                    Card(
                        onClick = {
                            viewModel.selectConversionType(tool.type)
                            val mimeType = when (tool.type) {
                                ConversionType.PDF_TO_IMAGE, ConversionType.PDF_TO_TEXT -> "application/pdf"
                                else -> "image/*"
                            }
                            filePickerLauncher.launch(mimeType)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = tool.gradientColors,
                                        start = Offset(0f, 0f),
                                        end = Offset(300f, 200f),
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    tool.icon,
                                    contentDescription = tool.type.displayName,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    tool.type.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // Progress
            if (uiState.isConverting) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Converting...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = GradientStart,
                        )
                    }
                }
            }

            // Result
            uiState.resultPath?.let { path ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Conversion Complete!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(path.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Filled.Share, "Share")
                        }
                    }
                }
            }
        }
    }
}
