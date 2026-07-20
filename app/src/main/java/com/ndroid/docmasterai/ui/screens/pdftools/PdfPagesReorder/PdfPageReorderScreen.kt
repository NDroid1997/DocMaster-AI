package com.ndroid.docmasterai.ui.screens.pdftools.pdfpagesreorder

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageReorderScreen(
    initialUri: Uri? = null,
    initialFileName: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PdfReorderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pages = viewModel.pages
    val isLoading by viewModel.isLoading
    val isSaving by viewModel.isSaving
    val error by viewModel.errorMessage
    val isSuccess by viewModel.isSuccess
    val sourceFileName by viewModel.sourceFileName

    LaunchedEffect(initialUri, initialFileName) {
        if (initialUri != null) {
            viewModel.loadPdf(initialUri, initialFileName ?: "document.pdf")
        }
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            Toast.makeText(context, "PDF reordered successfully", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.loadPdf(it, queryDisplayName(context, it) ?: "document.pdf")
        }
    }

    val gridState = rememberLazyGridState()
    val dragDropState = rememberGridDragDropState(gridState, onMove = viewModel::movePage)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reorder Pages") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pages.isNotEmpty()) {
                        IconButton(onClick = viewModel::resetOrder) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset order")
                        }
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
            if (pages.isEmpty() && !isLoading) {
                EmptyState(onPickPdf = { pickPdfLauncher.launch(arrayOf("application/pdf")) })
            } else {
                Text(text = sourceFileName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    text = if (isLoading) "Loading pages…" else "${pages.size} pages — long-press and drag to reorder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(dragDropState) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDropState.onDrag(dragAmount)
                                },
                                onDragEnd = { dragDropState.onDragInterrupted() },
                                onDragCancel = { dragDropState.onDragInterrupted() }
                            )
                        }
                ) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pages, key = { it.id }) { page ->
                            val index = pages.indexOf(page)
                            val isDragging = index == dragDropState.draggingItemIndex

                            PageCell(
                                page = page,
                                displayNumber = index + 1,
                                modifier = if (isDragging) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer {
                                            translationX = dragDropState.draggingItemOffset.x
                                            translationY = dragDropState.draggingItemOffset.y
                                            scaleX = 1.05f
                                            scaleY = 1.05f
                                        }
                                        .shadow(8.dp, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.animateItem()
                                }
                            )
                        }
                    }
                }

                error?.let {
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
                        onClick = viewModel::saveReorderedPdf,
                        enabled = pages.isNotEmpty() && !isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save Order")
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
        Text("Pick a PDF to reorder its pages", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onPickPdf) { Text("Choose PDF") }
    }
}

@Composable
private fun PageCell(
    page: PdfPageItem,
    displayNumber: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val bmp: Bitmap? = page.bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page $displayNumber",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        Text(
            text = "$displayNumber",
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
