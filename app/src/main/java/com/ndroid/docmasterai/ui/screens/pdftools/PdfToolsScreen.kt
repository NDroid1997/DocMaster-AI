package com.ndroid.docmasterai.ui.screens.pdftools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.ndroid.docmasterai.data.model.PdfOperationType
import com.ndroid.docmasterai.ui.theme.*

data class PdfTool(
    val type: PdfOperationType,
    val icon: ImageVector,
    val description: String,
    val gradientColors: List<Color>,
)

val pdfTools = listOf(
    PdfTool(PdfOperationType.MERGE, Icons.Filled.MergeType, "Combine multiple PDFs", listOf(CardGradient1Start, CardGradient1End)),
    PdfTool(PdfOperationType.SPLIT, Icons.Filled.CallSplit, "Split PDF by pages", listOf(CardGradient2Start, CardGradient2End)),
    PdfTool(PdfOperationType.ROTATE, Icons.Filled.RotateRight, "Rotate PDF pages", listOf(CardGradient3Start, CardGradient3End)),
    PdfTool(PdfOperationType.REORDER, Icons.Filled.Reorder, "Reorder pages", listOf(CardGradient4Start, CardGradient4End)),
    PdfTool(PdfOperationType.DELETE_PAGES, Icons.Filled.DeleteSweep, "Remove pages", listOf(CardGradient5Start, CardGradient5End)),
    PdfTool(PdfOperationType.COMPRESS, Icons.Filled.Compress, "Reduce file size", listOf(CardGradient1End, CardGradient2Start)),
    PdfTool(PdfOperationType.WATERMARK, Icons.Filled.BrandingWatermark, "Add watermark", listOf(CardGradient2End, CardGradient3Start)),
    PdfTool(PdfOperationType.PASSWORD_PROTECT, Icons.Filled.Lock, "Protect with password", listOf(CardGradient3End, CardGradient4Start)),
    PdfTool(PdfOperationType.REMOVE_PASSWORD, Icons.Filled.LockOpen, "Remove password", listOf(CardGradient4End, CardGradient5Start)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfToolsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSourcePdf(it) }
    }

    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.setSourcePdfs(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Tools", fontWeight = FontWeight.SemiBold) },
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
                "Select a PDF Tool",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(pdfTools) { tool ->
                    Card(
                        onClick = {
                            viewModel.selectTool(tool.type)
                            if (tool.type == PdfOperationType.MERGE) {
                                multiPdfPickerLauncher.launch("application/pdf")
                            } else {
                                pdfPickerLauncher.launch("application/pdf")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f),
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
                                        end = Offset(200f, 200f),
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Icon(
                                    tool.icon,
                                    contentDescription = tool.type.displayName,
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    tool.type.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }

            // Processing indicator
            if (uiState.isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Processing...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GradientStart)
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Done!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(path.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
