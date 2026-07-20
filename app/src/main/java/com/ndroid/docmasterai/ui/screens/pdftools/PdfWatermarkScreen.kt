package com.ndroid.docmasterai.ui.screens.pdftools

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfWatermarkScreen(
    uri: Uri,
    fileName: String,
    onNavigateBack: () -> Unit,
    viewModel: PdfWatermarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uri, fileName) {
        viewModel.initialize(uri, fileName)
    }

    LaunchedEffect(uiState.resultPath) {
        if (uiState.resultPath != null) {
            Toast.makeText(context, "Watermark added successfully!", Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Watermark", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BrandingWatermark,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Applying to: ${uiState.sourceFileName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = uiState.watermarkText,
                onValueChange = { viewModel.updateWatermarkText(it) },
                label = { Text("Watermark Text") },
                placeholder = { Text("e.g. CONFIDENTIAL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isProcessing
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This text will be added diagonally across every page of the PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            Button(
                onClick = { viewModel.applyWatermark() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isProcessing && uiState.watermarkText.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Watermark & Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
