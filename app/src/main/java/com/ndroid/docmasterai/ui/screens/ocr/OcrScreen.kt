package com.ndroid.docmasterai.ui.screens.ocr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onNavigateBack: () -> Unit,
    viewModel: OcrViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR - Text Recognition", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Pick image card
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
                                colors = listOf(CardGradient2Start, CardGradient2End),
                                start = Offset(0f, 0f),
                                end = Offset(500f, 300f),
                            ),
                            shape = MaterialTheme.shapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.TextFields,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Tap to select an image",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "Supports JPG, PNG, WEBP, PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language selector
            var expanded by remember { mutableStateOf(false) }
            val languages = listOf("Latin", "Chinese", "Devanagari", "Japanese", "Korean")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = uiState.selectedLanguage.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("OCR Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                viewModel.setLanguage(lang.lowercase())
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Processing state
            if (uiState.isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GradientStart,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Extracting text...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorContainer),
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(16.dp),
                        color = ErrorDark,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Result
            if (uiState.extractedText.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Extracted Text",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(uiState.extractedText))
                        }) {
                            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.shareText(context) }) {
                            Icon(Icons.Filled.Share, "Share", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (uiState.processingTimeMs > 0) {
                    Text(
                        "Processed in ${uiState.processingTimeMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = uiState.extractedText,
                    onValueChange = { viewModel.updateText(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Export options
                Text(
                    "Export As",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportAsTxt() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Description, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TXT")
                    }
                    OutlinedButton(
                        onClick = { viewModel.exportAsPdf() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: DOCX export */ },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Article, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DOCX")
                    }
                }
            }
        }
    }
}
