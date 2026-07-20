package com.ndroid.docmasterai.ui.screens.resize

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.data.model.ResizeMode
import com.ndroid.docmasterai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizeScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResizeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-navigate back on success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            val fileName = uiState.savedPath?.substringAfterLast("/") ?: "Image"
            snackbarHostState.showSnackbar(
                message = "Image saved to Downloads/DocMasterAi/$fileName",
                duration = SnackbarDuration.Long
            )
            viewModel.clearSuccess()
            onNavigateBack()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = "Error: $errorMsg",
                duration = SnackbarDuration.Long
            )
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resize Image", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Image picker
            if (uiState.selectedImageUri == null) {
                Card(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(CardGradient5Start, CardGradient5End),
                                    start = Offset(0f, 0f),
                                    end = Offset(500f, 300f),
                                ),
                                shape = MaterialTheme.shapes.medium,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PhotoSizeSelectLarge, null, modifier = Modifier.size(48.dp), tint = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Image to Resize", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            } else {
                // Resize controls
                Text("Resize Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                // Mode selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ResizeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = uiState.selectedMode == mode,
                            onClick = { viewModel.setMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ResizeMode.entries.size),
                        ) {
                            Text(mode.displayName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dimension inputs
                when (uiState.selectedMode) {
                    ResizeMode.WIDTH -> {
                        OutlinedTextField(
                            value = uiState.width,
                            onValueChange = { viewModel.setWidth(it) },
                            label = { Text("Width (px)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ResizeMode.HEIGHT -> {
                        OutlinedTextField(
                            value = uiState.height,
                            onValueChange = { viewModel.setHeight(it) },
                            label = { Text("Height (px)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    ResizeMode.PERCENTAGE -> {
                        Column {
                            Text("Scale: ${uiState.percentage}%", style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = uiState.percentage.toFloat(),
                                onValueChange = { viewModel.setPercentage(it.toInt()) },
                                valueRange = 10f..200f,
                                colors = SliderDefaults.colors(thumbColor = GradientStart, activeTrackColor = GradientStart),
                            )
                        }
                    }
                    ResizeMode.CUSTOM -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.width,
                                onValueChange = { viewModel.setWidth(it) },
                                label = { Text("Width") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = uiState.height,
                                onValueChange = { viewModel.setHeight(it) },
                                label = { Text("Height") },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Aspect ratio toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Maintain Aspect Ratio", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.maintainAspectRatio,
                        onCheckedChange = { viewModel.toggleAspectRatio() },
                        colors = SwitchDefaults.colors(checkedThumbColor = GradientStart),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quality slider
                Column {
                    Text("Quality: ${uiState.quality}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = uiState.quality.toFloat(),
                        onValueChange = { viewModel.setQuality(it.toInt()) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = GradientStart, activeTrackColor = GradientStart),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Output format
                Text("Output Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JPG", "PNG", "WEBP").forEach { format ->
                        FilterChip(
                            selected = uiState.outputFormat == format.lowercase(),
                            onClick = { viewModel.setOutputFormat(format.lowercase()) },
                            label = { Text(format) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GradientStart,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Resize button
                Button(
                    onClick = { viewModel.resize() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.isProcessing,
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.PhotoSizeSelectLarge, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resize Image", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
