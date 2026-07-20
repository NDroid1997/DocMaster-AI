package com.ndroid.docmasterai.ui.screens.pdftools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageSelectorScreen(
    initialUri: Uri? = null,
    initialFileName: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PdfPageSelectorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialUri, initialFileName) {
        if (initialUri != null && state.sourceUri == null) {
            viewModel.loadPdf(initialUri, initialFileName ?: "document.pdf")
         }
    }

    LaunchedEffect(state.resultFiles) {
        if (state.resultFiles.isNotEmpty()) {
            val message = if (state.resultFiles.size == 1) "PDF created successfully" else "${state.resultFiles.size} PDFs created"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.loadPdf(it, queryDisplayName(context, it) ?: "document.pdf")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Page Splitter") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (state.totalPages == 0 && !state.isLoadingPdf) {
                EmptyState(onPickPdf = { pickPdfLauncher.launch(arrayOf("application/pdf")) })
            } else {
                Text(text = state.sourceFileName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    text = if (state.isLoadingPdf) "Loading pages…" else "${state.totalPages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                ModeSelectionRow(selected = state.selectedMode, onSelect = viewModel::selectMode)
                Spacer(Modifier.height(12.dp))

                state.selectedMode?.let { mode ->
                    ModeInputSection(
                        mode = mode,
                        state = state,
                        onRangeChange = viewModel::updateRange,
                        onSpecificPagesChange = viewModel::updateSpecificPages
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${state.previewSelectedPages.size} of ${state.totalPages} pages selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (state.isLoadingPdf) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                             CircularProgressIndicator()
                         }
                    } else {
                        PageThumbnailGrid(
                            thumbnails = state.thumbnails,
                            selectedPages = state.previewSelectedPages,
                            interactive = state.selectedMode == SplitMode.CUSTOM_SELECTION,
                            onPageTap = viewModel::togglePageInCustomSelection
                        )
                    }
                }

                state.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Change PDF") }

                    Button(
                        onClick = viewModel::performOperation,
                        enabled = state.selectedMode != null && !state.isProcessing &&
                                (state.selectedMode == SplitMode.INDIVIDUAL_PAGES || state.previewSelectedPages.isNotEmpty()),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onPickPdf: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Pick a PDF to get started", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPickPdf) { Text("Choose PDF") }
    }
}

@Composable
private fun ModeSelectionRow(selected: SplitMode?, onSelect: (SplitMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SplitMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Surface(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(mode.title, fontWeight = FontWeight.SemiBold)
                    Text(mode.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ModeInputSection(
    mode: SplitMode,
    state: PdfSplitUiState,
    onRangeChange: (String, String) -> Unit,
    onSpecificPagesChange: (String) -> Unit
) {
    when (mode) {
        SplitMode.PAGE_RANGE -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.rangeStart,
                    onValueChange = { onRangeChange(it, state.rangeEnd) },
                    label = { Text("Start page") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.rangeEnd,
                    onValueChange = { onRangeChange(state.rangeStart, it) },
                    label = { Text("End page") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
        SplitMode.SPECIFIC_PAGES -> {
            OutlinedTextField(
                value = state.specificPagesText,
                onValueChange = onSpecificPagesChange,
                label = { Text("Pages, e.g. 1, 3, 7") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        SplitMode.CUSTOM_SELECTION -> {
            Text("Tap pages below to add or remove them from the new PDF.", style = MaterialTheme.typography.bodySmall)
        }
        SplitMode.INDIVIDUAL_PAGES -> {
            Text("Every page will be exported as its own single-page PDF.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PageThumbnailGrid(
    thumbnails: List<PdfPageThumbnail>,
    selectedPages: Set<Int>,
    interactive: Boolean,
    onPageTap: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(thumbnails, key = { it.pageIndex }) { thumb ->
            PageThumbnailCell(
                thumbnail = thumb,
                included = thumb.pageIndex in selectedPages,
                interactive = interactive,
                onTap = { onPageTap(thumb.pageIndex) }
            )
        }
    }
}

@Composable
private fun PageThumbnailCell(
    thumbnail: PdfPageThumbnail,
    included: Boolean,
    interactive: Boolean,
    onTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (included) 2.dp else 1.dp,
                color = if (included) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .then(if (interactive) Modifier.clickable { onTap() } else Modifier)
            .alpha(if (included) 1f else 0.45f)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val bmp: Bitmap? = thumbnail.bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${thumbnail.pageIndex + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            if (included) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Included",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
                )
            }
        }
        Text(
            text = "${thumbnail.pageIndex + 1}",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) it.getString(nameIndex) else null
    }
}
