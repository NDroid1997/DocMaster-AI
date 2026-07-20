package com.ndroid.docmasterai.ui.screens.premium

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ndroid.docmasterai.premium.PremiumManager
import com.ndroid.docmasterai.premium.TrialStatus
import com.ndroid.docmasterai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeScreen(
    onNavigateBack: () -> Unit,
    premiumManager: PremiumManager,
) {
    val isPremium by premiumManager.isPremium.collectAsState()
    val trialStatus by premiumManager.trialStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Experience DocMaster AI Without Limits",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Get access to all advanced tools and features",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Features List
            FeatureItem(Icons.Default.TextFields, "Unlimited OCR", "Extract text from any document without daily limits")
            FeatureItem(Icons.Default.Build, "Advanced PDF Tools", "Merge, split, reorder, and protect your PDFs")
            FeatureItem(Icons.Default.SwapHoriz, "Batch Conversion", "Convert multiple files at once to various formats")
            FeatureItem(Icons.Default.DoNotDisturb, "Ad-Free Experience", "Focused work environment with no interruptions")
            FeatureItem(Icons.Default.CloudUpload, "Cloud Sync", "Access your documents across all your devices")

            Spacer(modifier = Modifier.height(40.dp))

            // Trial / Purchase Logic
            if (isPremium) {
                PremiumActiveStatus()
            } else {
                when (trialStatus) {
                    is TrialStatus.Active -> {
                        TrialActiveStatus((trialStatus as TrialStatus.Active).daysRemaining)
                    }
                    else -> {
                        if (trialStatus == TrialStatus.NotStarted) {
                            Button(
                                onClick = { premiumManager.startTrial() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(containerColor = GradientStart)
                            ) {
                                Text("Start 7-Day Free Trial", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { premiumManager.launchPurchaseFlow() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Upgrade for ₹25", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "One-time payment for lifetime access",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.linearGradient(listOf(GradientStart, GradientMid)),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumActiveStatus() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Premium Lifetime Access Active", fontWeight = FontWeight.Bold, color = Success)
        }
    }
}

@Composable
fun TrialActiveStatus(daysRemaining: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GradientStart.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GradientStart)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Free Trial Active", fontWeight = FontWeight.Bold, color = GradientStart)
            }
            Text(
                "$daysRemaining days remaining in your trial",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, start = 36.dp)
            )
        }
    }
}
