package com.ndroid.docmasterai.ui.screens.scanner

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.ndroid.docmasterai.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onScanComplete: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {







//    var hasCameraPermission by remember {
//        mutableStateOf(
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.CAMERA
//            ) == PackageManager.PERMISSION_GRANTED
//        )
//    }
//
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { granted -> hasCameraPermission = granted }
//
//    LaunchedEffect(Unit) {
//        if (!hasCameraPermission) {
//            permissionLauncher.launch(Manifest.permission.CAMERA)
//        }
//    }
//
//    LaunchedEffect(uiState.scanCompleted) {
//        if (uiState.scanCompleted) {
//            onScanComplete(uiState.documentId)
//        }
//    }

//    Box(modifier = Modifier
//        .fillMaxSize()
//        .background(Color.Black)) {
////        if (hasCameraPermission) {
//            val previewView = remember { PreviewView(context) }
//            val preview = remember { Preview.Builder().build() }
//            val imageCapture = remember {
//                ImageCapture.Builder()
//                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//                    .build()
//            }
//            val imageAnalysis = remember {
//                ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .build()
//            }
//            val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
//
//            val options = remember {
//                ObjectDetectorOptions.Builder()
//                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//                    .enableClassification() // To detect if it's a "document" or something else
//                    .build()
//            }
//            val objectDetector = remember { ObjectDetection.getClient(options) }
//
//            LaunchedEffect(Unit) {
//                viewModel.setImageCapture(imageCapture)
//            }
//
//            LaunchedEffect(lifecycleOwner) {
//                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
//                cameraProviderFuture.addListener({
//                    val cameraProvider = cameraProviderFuture.get()
//                    try {
//                        cameraProvider.unbindAll()
//                        cameraProvider.bindToLifecycle(
//                            lifecycleOwner,
//                            cameraSelector,
//                            preview,
//                            imageCapture,
//                            imageAnalysis
//                        )
//                        preview.surfaceProvider = previewView.surfaceProvider
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }, ContextCompat.getMainExecutor(context))
//            }
//
//            LaunchedEffect(imageAnalysis) {
//                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
//                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
//                    val mediaImage = imageProxy.image
//                    if (mediaImage != null) {
//                        val image = InputImage.fromMediaImage(
//                            mediaImage,
//                            imageProxy.imageInfo.rotationDegrees
//                        )
//                        objectDetector.process(image)
//                            .addOnSuccessListener { detectedObjects ->
//                                // Find the largest object (likely the document)
//                                val largestObject =
//                                    detectedObjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
//                                viewModel.onDocumentDetected(largestObject?.boundingBox)
//                            }
//                            .addOnFailureListener {
//                                viewModel.onDocumentDetected(null)
//                            }
//                            .addOnCompleteListener {
//                                imageProxy.close()
//                            }
//                    } else {
//                        imageProxy.close()
//                    }
//                }
//            }
//
//            AndroidView(
//                factory = { previewView },
//                modifier = Modifier.fillMaxSize()
//            )
//
//            // Edge detection overlay
//            EdgeDetectionOverlay(
//                detectedRect = uiState.detectedRect,
//                modifier = Modifier.fillMaxSize(),
//            )
//
//            // Top bar
//            TopAppBar(
//                title = {
//                    Text(
//                        "Scanner",
//                        color = Color.White,
//                        fontWeight = FontWeight.SemiBold,
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
//                    }
//                },
//                actions = {
//                    IconButton(onClick = { viewModel.toggleFlash() }) {
//                        Icon(
//                            if (uiState.isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
//                            "Flash",
//                            tint = Color.White,
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
//            )
//
//            // Bottom controls
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
//                    .background(Color.Black.copy(alpha = 0.6f))
//                    .padding(vertical = 24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
//            ) {
//                // Page count
//                if (uiState.scannedPages.isNotEmpty()) {
//                    Text(
//                        text = "${uiState.scannedPages.size} page(s) scanned",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = Color.White,
//                    )
//                    Spacer(modifier = Modifier.height(12.dp))
//                }
//
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    // Gallery import
//                    IconButton(
//                        onClick = { /* Import from gallery */ },
//                        modifier = Modifier
//                            .size(48.dp)
//                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
//                    ) {
//                        Icon(Icons.Filled.PhotoLibrary, "Gallery", tint = Color.White)
//                    }
//
//                    // Capture button
//                    Button(
//                        onClick = { viewModel.captureImage() },
//                        modifier = Modifier
//                            .size(72.dp)
//                            .border(3.dp, Color.White, CircleShape),
//                        shape = CircleShape,
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
//                        enabled = !uiState.isCapturing,
//                    ) {
//                        if (uiState.isCapturing) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(24.dp),
//                                color = Color.White,
//                                strokeWidth = 2.dp,
//                            )
//                        }
//                    }
//
//                    // Done button (when pages > 0)
//                    if (uiState.scannedPages.isNotEmpty()) {
//                        Button(
//                            onClick = { viewModel.finishScanning() },
//                            shape = CircleShape,
//                            modifier = Modifier.size(48.dp),
//                            colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
//                            contentPadding = PaddingValues(0.dp),
//                            enabled = !uiState.isSaving,
//                        ) {
//                            if (uiState.isSaving) {
//                                CircularProgressIndicator(
//                                    modifier = Modifier.size(24.dp),
//                                    color = Color.White,
//                                    strokeWidth = 2.dp,
//                                )
//                            } else {
//                                Icon(Icons.Filled.Check, "Done", tint = Color.White)
//                            }
//                        }
//                    } else {
//                        Spacer(modifier = Modifier.size(48.dp))
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Batch mode toggle
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.Center,
//                    modifier = Modifier.fillMaxWidth(),
//                ) {
//                    Text(
//                        "Batch Mode",
//                        color = Color.White,
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Switch(
//                        checked = uiState.isBatchMode,
//                        onCheckedChange = { viewModel.toggleBatchMode() },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = GradientStart,
//                            checkedTrackColor = GradientStart.copy(alpha = 0.3f),
//                        ),
//                    )
//                }
//            }
////        } else {
//            // Permission denied state
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(32.dp),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center,
//            ) {
//                Icon(
//                    Icons.Filled.CameraAlt,
//                    contentDescription = null,
//                    modifier = Modifier.size(80.dp),
//                    tint = Color.White.copy(alpha = 0.5f),
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//                Text(
//                    "Camera permission required",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = Color.White,
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Button(onClick = {
////                    permissionLauncher.launch(Manifest.permission.CAMERA)
//                }) {
//                    Text("Grant Permission")
//                }
//            }
////        }
//    }
//}
//
//@Composable
//fun EdgeDetectionOverlay(
//    detectedRect: Rect?,
//    modifier: Modifier = Modifier
//) {
//    val infiniteTransition = rememberInfiniteTransition(label = "scan")
//    val animAlpha by infiniteTransition.animateFloat(
//        initialValue = 0.3f,
//        targetValue = 0.7f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(1500),
//            repeatMode = RepeatMode.Reverse,
//        ),
//        label = "scanAlpha",
//    )
//
//    val guideColor = GradientStart.copy(alpha = animAlpha)
//
//    Box(modifier = modifier) {
//        if (detectedRect != null) {
//            // Draw detected rectangle
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                // Simplified coordinate mapping (assumes 480x640 analysis resolution)
//                // In a real app, you'd use ViewPort and calculate ratios
//                val scaleX = size.width / 480f
//                val scaleY = size.height / 640f
//
//                drawRect(
//                    color = guideColor,
//                    topLeft = Offset(detectedRect.left * scaleX, detectedRect.top * scaleY),
//                    size = Size(
//                        (detectedRect.right - detectedRect.left) * scaleX,
//                        (detectedRect.bottom - detectedRect.top) * scaleY
//                    ),
//                    style = Stroke(width = 3.dp.toPx())
//                )
//            }
//        } else {
//            // Corner guides (Fallback when no object detected)
//            val guideWidth = 3.dp
//            val guideLength = 40.dp
//            val margin = 60.dp
//
//            // Top-left
//            Box(
//                modifier = Modifier
//                    .offset(x = margin, y = margin + 80.dp)
//                    .size(guideLength, guideWidth)
//                    .background(guideColor)
//            )
//            Box(
//                modifier = Modifier
//                    .offset(x = margin, y = margin + 80.dp)
//                    .size(guideWidth, guideLength)
//                    .background(guideColor)
//            )
//
//            // Top-right
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .offset(x = -margin, y = margin + 80.dp)
//                    .size(guideLength, guideWidth)
//                    .background(guideColor)
//            )
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .offset(x = -margin, y = margin + 80.dp)
//                    .size(guideWidth, guideLength)
//                    .background(guideColor)
//            )
//
//            // Bottom-left
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomStart)
//                    .offset(x = margin, y = -margin - 120.dp)
//                    .size(guideLength, guideWidth)
//                    .background(guideColor)
//            )
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomStart)
//                    .offset(x = margin, y = -margin - 120.dp)
//                    .size(guideWidth, guideLength)
//                    .background(guideColor)
//            )
//
//            // Bottom-right
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .offset(x = -margin, y = -margin - 120.dp)
//                    .size(guideLength, guideWidth)
//                    .background(guideColor)
//            )
//            Box(
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .offset(x = -margin, y = -margin - 120.dp)
//                    .size(guideWidth, guideLength)
//                    .background(guideColor)
//            )
//        }
//    }
}
