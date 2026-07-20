package com.ndroid.docmasterai.ui.screens.home

import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.ui.theme.*
import com.ndroid.docmasterai.utils.GlobalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScanner: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToConverter: () -> Unit,
    onNavigateToPdfTools: () -> Unit,
    onNavigateToResize: () -> Unit,
    onNavigateToFileManager: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onDocItemClick:(documentId: String, images: List<Uri>, pdf: Uri?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val favoriteDocs by viewModel.favoriteDocuments.collectAsState()
    val docCount by viewModel.documentCount.collectAsState()
    val totalStorage by viewModel.totalStorage.collectAsState()
    val isPremium by viewModel.premiumManager.isPremium.collectAsState()
    val trialStatus by viewModel.premiumManager.trialStatus.collectAsState()

    var isFeatureAvailable by remember { mutableStateOf(true) }
// val isFeatureAvailable = remember(isPremium, trialStatus) {
//        isPremium || trialStatus is com.ndroid.docmasterai.premium.TrialStatus.Active
//    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "DocMaster AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "$docCount documents • ${formatSize(totalStorage)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Grid
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard(
                icon = Icons.Filled.DocumentScanner,
                title = "Scan",
                gradientColors = listOf(CardGradient1Start, CardGradient1End),
                onClick = onNavigateToScanner,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.TextFields,
                title = "OCR",
                gradientColors = listOf(CardGradient2Start, CardGradient2End),
                onClick = onNavigateToOcr,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.SwapHoriz,
                title = "Convert",
                gradientColors = listOf(CardGradient3Start, CardGradient3End),
                onClick = if (isFeatureAvailable) onNavigateToConverter else onNavigateToPremium,
                modifier = Modifier.weight(1f),
                isLocked = !isFeatureAvailable
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickActionCard(
                icon = Icons.Filled.PictureAsPdf,
                title = "PDF Tools",
                gradientColors = listOf(CardGradient4Start, CardGradient4End),
                onClick = if (isFeatureAvailable) onNavigateToPdfTools else onNavigateToPremium,
                modifier = Modifier.weight(1f),
                isLocked = !isFeatureAvailable
            )
            QuickActionCard(
                icon = Icons.Filled.PhotoSizeSelectLarge,
                title = "Resize",
                gradientColors = listOf(CardGradient5Start, CardGradient5End),
                onClick = onNavigateToResize,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                icon = Icons.Filled.FolderOpen,
                title = "Files",
                gradientColors = listOf(CardGradient1End, CardGradient2Start),
                onClick = onNavigateToFileManager,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Premium Banner
        if (!isPremium) {
            val trialText = when (val status = trialStatus) {
                is com.ndroid.docmasterai.premium.TrialStatus.Active -> "Trial ends in ${status.daysRemaining} days"
                else -> "Unlock all tools & features"
            }
            PremiumBanner(
                modifier = Modifier.padding(horizontal = 16.dp),
                description = trialText,
                onClick = onNavigateToPremium
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Documents
        if (recentDocs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onNavigateToFileManager) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(recentDocs, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                        onDocItemClick = onDocItemClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Favorites
        if (favoriteDocs.isNotEmpty()) {
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(favoriteDocs, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                        onDocItemClick = onDocItemClick
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Empty state
        if (recentDocs.isEmpty() && favoriteDocs.isEmpty()) {
            EmptyHomeState(
                onScan = onNavigateToScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(200f, 200f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                    if (isLocked) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(14.dp).offset(x = 8.dp, y = (-8).dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: DocumentEntity,
    onFavoriteToggle: () -> Unit,
    onDocItemClick:(documentId: String, images: List<Uri>, pdf: Uri?) -> Unit,
    modifier: Modifier = Modifier,
) {

    val globalViewModel = hiltViewModel<GlobalViewModel>()

    Card(
        modifier = modifier.width(160.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        onClick = {
            globalViewModel.prepareForEditing(document, onDocItemClick)
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (document.thumbnailPath != null) {
                    AsyncImage(
                        model = java.io.File(document.thumbnailPath),
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = when (document.fileType) {
                            "pdf" -> Icons.Filled.PictureAsPdf
                            "jpg", "jpeg", "png", "webp" -> Icons.Filled.Image
                            else -> Icons.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
//                Icon(
//                    imageVector = when (document.fileType) {
//                        "pdf" -> Icons.Filled.PictureAsPdf
//                        "jpg", "jpeg", "png", "webp" -> Icons.Filled.Image
//                        else -> Icons.Filled.InsertDriveFile
//                    },
//                    contentDescription = null,
//                    modifier = Modifier.size(36.dp),
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDate(document.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(16.dp),
                        tint = if (document.isFavorite) TertiaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBanner(
    modifier: Modifier = Modifier,
    description: String = "Unlimited OCR, batch conversion, advanced PDF tools & more",
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientMid),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 200f),
                    ),
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unlock Premium ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = onClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Upgrade", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No documents yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan your first document to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onScan,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Document")
        }
    }
}




private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
