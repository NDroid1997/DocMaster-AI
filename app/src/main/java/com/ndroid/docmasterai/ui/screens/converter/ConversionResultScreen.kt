package com.ndroid.docmasterai.ui.screens.converter

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ndroid.docmasterai.ui.theme.Success
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionResultScreen(
    resultPath: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    
    // Resolve the actual file path. 
    // If it's a "User Visible Path" (Download/DocMasterAi/...), we need to find the real file.
    // For now, if it's text, we expect it to be registered in the internal storage for preview.
    
    val file = remember(resultPath) {
        if (resultPath.startsWith("Download/")) {
            // This is tricky because we don't have direct File access to MediaStore paths easily.
            // However, our repository saves a copy in 'internalStorage/conversions'.
            // Let's assume for preview we check the internal conversions folder first.
            val fileName = resultPath.substringAfterLast("/")
            val internalFile = File(context.filesDir, "conversions/$fileName")
            if (internalFile.exists()) internalFile else File(resultPath)
         } else {
            File(resultPath)
        }
    }
    val isText = file.extension.lowercase() == "txt"
    val content = remember(resultPath) {
        if (isText && file.exists()) file.readText() else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversion Result", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (isText) "text/plain" else "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Result"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Success
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Conversion Successful!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isText && content != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Extracted Text", fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Extracted Text", content)
                                clipboard.setPrimaryClip(clip)
                            }) {
                                Icon(Icons.Default.ContentCopy, "Copy")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            SelectionContainer {
                                Text(content)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("The file has been saved to your downloads folder.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Done")
            }
        }
    }
}
