package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.file.FileItem
import com.example.data.file.FileType
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.FileListItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    currentDir: File,
    baseDir: File,
    files: List<FileItem>,
    searchQuery: String,
    isGridView: Boolean,
    selectedFilePaths: Set<String> = emptySet(),
    onToggleFileSelection: (String) -> Unit = {},
    onClearFileSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onZipSelected: () -> Unit = {},
    onUnzipFile: (FileItem) -> Unit = {},
    onShowDetails: (FileItem) -> Unit = {},
    onSearchChange: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onNavigateToDir: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenExcel: (FileItem) -> Unit,
    onOpenText: (FileItem) -> Unit,
    onEncrypt: (FileItem) -> Unit,
    onDecrypt: (FileItem) -> Unit,
    onUploadFtp: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onCreateFolderClick: () -> Unit,
    onCreateFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filterType by remember { mutableStateOf<String>("ALL") }

    val filteredFiles = remember(files, searchQuery, filterType) {
        files.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterType) {
                "EXCEL" -> item.fileType == FileType.EXCEL
                "TEXT" -> item.fileType == FileType.TEXT
                "VAULT" -> item.isEncrypted || item.fileType == FileType.ENCRYPTED
                "ARCHIVE" -> item.fileType == FileType.ARCHIVE
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onCreateFileClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("fab_browser_file")
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = "ملف جديد")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = onCreateFolderClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_browser_folder")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "مجلد جديد")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Batch selection action bar if files selected
            if (selectedFilePaths.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "تم تحديد ${selectedFilePaths.size} عناصر",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row {
                            IconButton(onClick = onZipSelected) {
                                Icon(Icons.Default.FolderZip, contentDescription = "ضغط ZIP", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف المحددة", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = onClearFileSelection) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد")
                            }
                        }
                    }
                }
            }

            // Search Bar & View Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("بحث في الملفات...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_file_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "تغيير العرض"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Breadcrumb Navigation Bar
            BreadcrumbBar(
                currentDir = currentDir,
                baseDir = baseDir,
                onNavigateToDir = onNavigateToDir
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == "ALL",
                    onClick = { filterType = "ALL" },
                    label = { Text("الكل") }
                )
                FilterChip(
                    selected = filterType == "EXCEL",
                    onClick = { filterType = "EXCEL" },
                    label = { Text("Excel/CSV") }
                )
                FilterChip(
                    selected = filterType == "TEXT",
                    onClick = { filterType = "TEXT" },
                    label = { Text("نصوص") }
                )
                FilterChip(
                    selected = filterType == "VAULT",
                    onClick = { filterType = "VAULT" },
                    label = { Text("مشفر") }
                )
                FilterChip(
                    selected = filterType == "ARCHIVE",
                    onClick = { filterType = "ARCHIVE" },
                    label = { Text("أرشيف ZIP") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File List or Empty State
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد ملفات في هذا المجلد",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFiles) { item ->
                            FileListItem(
                                item = item,
                                isSelected = selectedFilePaths.contains(item.path),
                                onToggleSelect = { onToggleFileSelection(item.path) },
                                onClick = { onOpenFile(item) },
                                onOpenExcel = { onOpenExcel(item) },
                                onOpenText = { onOpenText(item) },
                                onEncrypt = { onEncrypt(item) },
                                onDecrypt = { onDecrypt(item) },
                                onUnzip = { onUnzipFile(item) },
                                onShowDetails = { onShowDetails(item) },
                                onUploadFtp = { onUploadFtp(item) },
                                onRename = { onRename(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(filteredFiles) { item ->
                            FileListItem(
                                item = item,
                                isSelected = selectedFilePaths.contains(item.path),
                                onToggleSelect = { onToggleFileSelection(item.path) },
                                onClick = { onOpenFile(item) },
                                onOpenExcel = { onOpenExcel(item) },
                                onOpenText = { onOpenText(item) },
                                onEncrypt = { onEncrypt(item) },
                                onDecrypt = { onDecrypt(item) },
                                onUnzip = { onUnzipFile(item) },
                                onShowDetails = { onShowDetails(item) },
                                onUploadFtp = { onUploadFtp(item) },
                                onRename = { onRename(item) },
                                onDelete = { onDelete(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
