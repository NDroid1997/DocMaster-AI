package com.ndroid.docmasterai.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ndroid.docmasterai.data.model.ImageFilter
import com.ndroid.docmasterai.ui.theme.GradientStart
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    documentId: String,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: ScanResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(documentId) {
        viewModel.setDocumentId(documentId)
    }
    
    var selectedFilter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Scan", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSaveComplete) {
                        Text("Save", fontWeight = FontWeight.Bold, color = GradientStart)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Preview area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.pages.isNotEmpty()) {
                            val currentPage = uiState.pages[uiState.currentPageIndex]
                            AsyncImage(
                                model = File(currentPage.enhancedImagePath ?: currentPage.imagePath),
                                contentDescription = "Scanned Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Scanned Image",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }

            // Filter selector
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(ImageFilter.entries.toList()) { _, filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { 
                            selectedFilter = filter 
                            viewModel.applyFilter(filter)
                        },
                        label = { Text(filter.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GradientStart,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brightness slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Brightness", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(thumbColor = GradientStart, activeTrackColor = GradientStart),
                )
            }

            // Contrast slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Contrast", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = 0.5f..3f,
                    colors = SliderDefaults.colors(thumbColor = GradientStart, activeTrackColor = GradientStart),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = { /* Rotate */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.RotateRight, "Rotate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { /* Crop */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Crop, "Crop", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Crop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { /* Add page */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Add Page", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
