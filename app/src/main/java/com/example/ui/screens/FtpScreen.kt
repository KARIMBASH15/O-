package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.file.FileType
import com.example.data.file.RemoteFileItem
import com.example.data.local.FtpConnectionEntity
import com.example.data.local.SyncJobEntity
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.FtpPurple
import com.example.ui.theme.TextBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpScreen(
    connections: List<FtpConnectionEntity>,
    activeConnection: FtpConnectionEntity?,
    remoteCurrentPath: String,
    remoteFiles: List<RemoteFileItem>,
    syncJobs: List<SyncJobEntity>,
    isLoading: Boolean,
    onAddConnectionClick: () -> Unit,
    onDeleteConnection: (FtpConnectionEntity) -> Unit,
    onTestConnection: (FtpConnectionEntity) -> Unit,
    onSelectConnection: (FtpConnectionEntity) -> Unit,
    onNavigateRemoteDir: (String) -> Unit,
    onDownloadFile: (RemoteFileItem) -> Unit,
    onCloudEditFile: (RemoteFileItem) -> Unit,
    onAddSyncJobClick: () -> Unit,
    onRunSyncJob: (SyncJobEntity) -> Unit,
    onDeleteSyncJob: (SyncJobEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Servers, 1: Remote Browser, 2: Sync Jobs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tab Row Header
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("سيرفرات FTP") },
                icon = { Icon(Icons.Default.Dns, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("مستكشف Cloud") },
                icon = { Icon(Icons.Default.CloudQueue, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("المزامنة السحابية") },
                icon = { Icon(Icons.Default.Sync, null) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> FtpServersTab(
                connections = connections,
                onAddClick = onAddConnectionClick,
                onDelete = onDeleteConnection,
                onTest = onTestConnection,
                onConnect = { conn ->
                    onSelectConnection(conn)
                    selectedTab = 1
                }
            )
            1 -> RemoteBrowserTab(
                activeConnection = activeConnection,
                remoteCurrentPath = remoteCurrentPath,
                remoteFiles = remoteFiles,
                isLoading = isLoading,
                onNavigateDir = onNavigateRemoteDir,
                onDownload = onDownloadFile,
                onCloudEdit = onCloudEditFile,
                onSwitchServer = { selectedTab = 0 }
            )
            2 -> SyncJobsTab(
                syncJobs = syncJobs,
                onAddJobClick = onAddSyncJobClick,
                onRunJob = onRunSyncJob,
                onDeleteJob = onDeleteSyncJob
            )
        }
    }
}

@Composable
private fun FtpServersTab(
    connections: List<FtpConnectionEntity>,
    onAddClick: () -> Unit,
    onDelete: (FtpConnectionEntity) -> Unit,
    onTest: (FtpConnectionEntity) -> Unit,
    onConnect: (FtpConnectionEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سيرفرات الاتصال السحابي FTP / FTPS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = FtpPurple),
                modifier = Modifier.testTag("btn_add_ftp")
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة سيرفر")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (connections.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Dns, null, modifier = Modifier.size(48.dp), tint = FtpPurple)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لم يتم إضافة أي سيرفر FTP بعد. قم بإضافة بيانات السيرفر لتصفح وتعديل الملفات سحابياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(connections) { conn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Cloud, null, tint = FtpPurple)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(conn.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("${conn.username}@${conn.host}:${conn.port}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { onDelete(conn) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { onTest(conn) }) {
                                    Text("اختبار الاتصال")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onConnect(conn) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FtpPurple)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصفح الملفات")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteBrowserTab(
    activeConnection: FtpConnectionEntity?,
    remoteCurrentPath: String,
    remoteFiles: List<RemoteFileItem>,
    isLoading: Boolean,
    onNavigateDir: (String) -> Unit,
    onDownload: (RemoteFileItem) -> Unit,
    onCloudEdit: (RemoteFileItem) -> Unit,
    onSwitchServer: () -> Unit
) {
    if (activeConnection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("لم يتم تحديد أي سيرفر FTP متصل.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSwitchServer) {
                    Text("اختر سيرفر FTP للاتصال")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Active server banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FtpPurple.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("متصل بـ: ${activeConnection.name}", fontWeight = FontWeight.Bold, color = FtpPurple)
                    Text("المسار السحابي: $remoteCurrentPath", fontSize = 12.sp)
                }
                TextButton(onClick = onSwitchServer) {
                    Text("تغيير السيرفر")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FtpPurple)
            }
        } else if (remoteFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("المجلد السحابي فارغ أو يتعذر القراءة")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(remoteFiles) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.isDirectory) {
                                    onNavigateDir(item.remotePath)
                                } else if (item.fileType == FileType.EXCEL || item.fileType == FileType.TEXT) {
                                    onCloudEdit(item)
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, color) = if (item.isDirectory) {
                                Icons.Default.Folder to FtpPurple
                            } else if (item.fileType == FileType.EXCEL) {
                                Icons.Default.TableChart to ExcelGreen
                            } else {
                                Icons.Default.Description to TextBlue
                            }

                            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                if (!item.isDirectory) {
                                    Text("الحجم: ${item.size} B", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!item.isDirectory) {
                                if (item.fileType == FileType.EXCEL || item.fileType == FileType.TEXT) {
                                    OutlinedButton(
                                        onClick = { onCloudEdit(item) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل سحابي", fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                IconButton(onClick = { onDownload(item) }) {
                                    Icon(Icons.Default.Download, null, tint = FtpPurple)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncJobsTab(
    syncJobs: List<SyncJobEntity>,
    onAddJobClick: () -> Unit,
    onRunJob: (SyncJobEntity) -> Unit,
    onDeleteJob: (SyncJobEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مهام المزامنة التلقائية للملفات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddJobClick,
                colors = ButtonDefaults.buttonColors(containerColor = TextBlue),
                modifier = Modifier.testTag("btn_add_sync")
            ) {
                Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مهمة جديدة")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (syncJobs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مهام مزامنة محددة. يمكنك ضبط مزامنة المجلدات المحلية مع السيرفر FTP تلقائياً.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(syncJobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المزامنة [${job.syncDirection}]", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onDeleteJob(job) }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("المحلي: ${job.localPath}", fontSize = 12.sp)
                            Text("السحابي: ${job.remotePath}", fontSize = 12.sp)

                            if (job.lastSyncLog.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        job.lastSyncLog,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onRunJob(job) },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = TextBlue)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تطبيق المزامنة الآن")
                            }
                        }
                    }
                }
            }
        }
    }
}
