package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.FileItem
import com.example.data.file.StorageStats
import com.example.ui.components.FileListItem
import com.example.ui.components.StorageCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    storageStats: StorageStats,
    recentFiles: List<FileItem>,
    onOpenCategory: (String) -> Unit,
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
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onCreateFileClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("fab_create_file")
                ) {
                    Icon(Icons.Default.AddTask, contentDescription = "ملف جديد")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = onCreateFolderClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("fab_create_folder")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "مجلد جديد")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Storage Overview Header Card
            item {
                StorageCard(
                    stats = storageStats,
                    onQuickAccessClick = onOpenCategory
                )
            }

            // Core Capabilities Shortcut Grid
            item {
                Text(
                    text = "الأقسام الرئيسية والتعديل السحابي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickCapabilityCard(
                            title = "جداول Excel",
                            subtitle = "عرض وتعديل .xlsx & .csv",
                            icon = Icons.Default.TableChart,
                            color = ExcelGreen,
                            onClick = { onOpenCategory("EXCEL") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickCapabilityCard(
                            title = "الاتصال FTP",
                            subtitle = "سيرفرات والتعديل السحابي",
                            icon = Icons.Default.CloudSync,
                            color = FtpPurple,
                            onClick = { onOpenCategory("FTP") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickCapabilityCard(
                            title = "الخزنة المشفرة",
                            subtitle = "تشفير آمن AES-256",
                            icon = Icons.Default.Lock,
                            color = VaultAmber,
                            onClick = { onOpenCategory("VAULT") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickCapabilityCard(
                            title = "متصفح الجهاز",
                            subtitle = "استعراض كافة الملفات",
                            icon = Icons.Default.Folder,
                            color = TextBlue,
                            onClick = { onOpenCategory("ALL") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Files Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الملفات الأخيرة بالجهاز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { onOpenCategory("ALL") }) {
                        Text("عرض الكل")
                    }
                }
            }

            if (recentFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد ملفات حالية. انقر على الزر + لإنشاء مجلد أو جدول Excel جديد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentFiles.take(8)) { item ->
                    FileListItem(
                        item = item,
                        onClick = { onOpenFile(item) },
                        onOpenExcel = { onOpenExcel(item) },
                        onOpenText = { onOpenText(item) },
                        onEncrypt = { onEncrypt(item) },
                        onDecrypt = { onDecrypt(item) },
                        onUploadFtp = { onUploadFtp(item) },
                        onRename = { onRename(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
}

@Composable
private fun QuickCapabilityCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
