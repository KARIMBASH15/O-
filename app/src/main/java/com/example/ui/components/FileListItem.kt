package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.FileItem
import com.example.data.file.FileType
import com.example.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onOpenExcel: () -> Unit,
    onOpenText: () -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onUnzip: () -> Unit = {},
    onShowDetails: () -> Unit = {},
    onUploadFtp: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("file_item_${item.name}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.padding(end = 6.dp)
            )

            // Icon Badge
            FileIconBadge(fileType = item.fileType, isEncrypted = item.isEncrypted)

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isEncrypted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "مشفر",
                            tint = VaultAmber,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isDirectory) "مجلد" else item.formattedSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${item.formattedDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick primary action button
            if (item.fileType == FileType.EXCEL) {
                IconButton(onClick = onOpenExcel, modifier = Modifier.testTag("btn_open_excel")) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "فتح Excel",
                        tint = ExcelGreen
                    )
                }
            } else if (item.fileType == FileType.TEXT) {
                IconButton(onClick = onOpenText, modifier = Modifier.testTag("btn_open_text")) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "تعديل نص",
                        tint = TextBlue
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات الإجراءات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FileActionDropdown(
                    expanded = showMenu,
                    onDismiss = { showMenu = false },
                    item = item,
                    onOpenExcel = onOpenExcel,
                    onOpenText = onOpenText,
                    onEncrypt = onEncrypt,
                    onDecrypt = onDecrypt,
                    onUnzip = onUnzip,
                    onShowDetails = onShowDetails,
                    onUploadFtp = onUploadFtp,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun FileIconBadge(fileType: FileType, isEncrypted: Boolean) {
    val (icon, color) = when {
        isEncrypted || fileType == FileType.ENCRYPTED -> Icons.Default.Lock to VaultAmber
        fileType == FileType.DIRECTORY -> Icons.Default.Folder to Color(0xFFEAB308)
        fileType == FileType.EXCEL -> Icons.Default.TableChart to ExcelGreen
        fileType == FileType.TEXT -> Icons.Default.Description to TextBlue
        fileType == FileType.IMAGE -> Icons.Default.Image to MediaRose
        fileType == FileType.AUDIO -> Icons.Default.AudioFile to FtpPurple
        fileType == FileType.VIDEO -> Icons.Default.VideoFile to MediaRose
        fileType == FileType.PDF -> Icons.Default.PictureInPicture to Color(0xFFEF4444)
        fileType == FileType.ARCHIVE -> Icons.Default.FolderZip to VaultAmber
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FileActionDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: FileItem,
    onOpenExcel: () -> Unit,
    onOpenText: () -> Unit,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onUnzip: () -> Unit,
    onShowDetails: () -> Unit,
    onUploadFtp: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (item.fileType == FileType.EXCEL) {
            DropdownMenuItem(
                text = { Text("محرر Excel والجدول") },
                onClick = { onDismiss(); onOpenExcel() },
                leadingIcon = { Icon(Icons.Default.TableChart, null, tint = ExcelGreen) }
            )
        }
        if (item.fileType == FileType.TEXT) {
            DropdownMenuItem(
                text = { Text("المحرر النصي") },
                onClick = { onDismiss(); onOpenText() },
                leadingIcon = { Icon(Icons.Default.EditNote, null, tint = TextBlue) }
            )
        }
        if (item.fileType == FileType.ARCHIVE) {
            DropdownMenuItem(
                text = { Text("فك ضغط الأرشيف (Unzip)") },
                onClick = { onDismiss(); onUnzip() },
                leadingIcon = { Icon(Icons.Default.FolderZip, null, tint = VaultAmber) }
            )
        }
        if (!item.isDirectory && !item.isEncrypted) {
            DropdownMenuItem(
                text = { Text("تشفير آمن AES-256") },
                onClick = { onDismiss(); onEncrypt() },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = VaultAmber) }
            )
        }
        if (item.isEncrypted || item.fileType == FileType.ENCRYPTED) {
            DropdownMenuItem(
                text = { Text("فك تشفير الملف") },
                onClick = { onDismiss(); onDecrypt() },
                leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = VaultAmber) }
            )
        }
        if (!item.isDirectory) {
            DropdownMenuItem(
                text = { Text("رفع إلى سيرفر FTP") },
                onClick = { onDismiss(); onUploadFtp() },
                leadingIcon = { Icon(Icons.Default.CloudUpload, null, tint = FtpPurple) }
            )
        }
        DropdownMenuItem(
            text = { Text("عرض التفاصيل والمعلومات") },
            onClick = { onDismiss(); onShowDetails() },
            leadingIcon = { Icon(Icons.Default.Info, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("إعادة التسمية") },
            onClick = { onDismiss(); onRename() },
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
        )
        DropdownMenuItem(
            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}
