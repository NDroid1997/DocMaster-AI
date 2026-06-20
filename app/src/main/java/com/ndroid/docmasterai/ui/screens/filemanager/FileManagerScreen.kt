package com.ndroid.docmasterai.ui.screens.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.data.local.entity.DocumentEntity
import com.ndroid.docmasterai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class FileTab(val title: String) {
    ALL("All"), RECENT("Recent"), FAVORITES("Favorites"), TRASH("Trash")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: FileManagerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(FileTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val displayDocs = when (selectedTab) {
        FileTab.ALL -> uiState.allDocuments
        FileTab.RECENT -> uiState.recentDocuments
        FileTab.FAVORITES -> uiState.favoriteDocuments
        FileTab.TRASH -> uiState.deletedDocuments
    }.filter { doc ->
        searchQuery.isEmpty() || doc.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { isSearchActive = false },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search documents...") },
                    leadingIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {}
            } else {
                TopAppBar(
                    title = { Text("File Manager", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, "Search")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (uiState.isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                                "Toggle View",
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createFolder("New Folder") },
                containerColor = GradientStart,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Filled.CreateNewFolder, "New Folder")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = FileTab.entries.indexOf(selectedTab),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                FileTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            // Sort options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.sortBy == "date",
                    onClick = { viewModel.setSortBy("date") },
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(14.dp)) },
                )
                FilterChip(
                    selected = uiState.sortBy == "name",
                    onClick = { viewModel.setSortBy("name") },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Filled.SortByAlpha, null, modifier = Modifier.size(14.dp)) },
                )
                FilterChip(
                    selected = uiState.sortBy == "size",
                    onClick = { viewModel.setSortBy("size") },
                    label = { Text("Size") },
                    leadingIcon = { Icon(Icons.Filled.Storage, null, modifier = Modifier.size(14.dp)) },
                )
            }

            if (displayDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No documents found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayDocs, key = { it.id }) { doc ->
                        FileListItem(
                            document = doc,
                            isTrash = selectedTab == FileTab.TRASH,
                            onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                            onDelete = {
                                if (selectedTab == FileTab.TRASH) {
                                    viewModel.permanentlyDelete(doc.id)
                                } else {
                                    viewModel.softDelete(doc.id)
                                }
                            },
                            onRestore = { viewModel.restoreDocument(doc.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    document: DocumentEntity,
    isTrash: Boolean,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // File icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when (document.fileType) {
                        "pdf" -> Icons.Filled.PictureAsPdf
                        "jpg", "jpeg", "png", "webp" -> Icons.Filled.Image
                        else -> Icons.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = when (document.fileType) {
                        "pdf" -> ErrorDark
                        else -> GradientStart
                    },
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${document.fileType.uppercase()} • ${formatSize(document.fileSize)} • ${formatDate(document.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isTrash) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Filled.RestoreFromTrash, "Restore", modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (document.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        "Favorite",
                        modifier = Modifier.size(20.dp),
                        tint = if (document.isFavorite) TertiaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
