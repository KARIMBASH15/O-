package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.file.FileItem
import com.example.data.file.FileType
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.FtpPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultAmber
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiMessage
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FileMasterApp()
            }
        }
    }
}

enum class AppScreen {
    HOME,
    FILES,
    FTP,
    VAULT,
    EXCEL_EDITOR,
    TEXT_EDITOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileMasterApp(viewModel: MainViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    val currentDir by viewModel.currentDir.collectAsStateWithLifecycle()
    val fileItems by viewModel.fileItems.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val ftpConnections by viewModel.ftpConnections.collectAsStateWithLifecycle()
    val syncJobs by viewModel.syncJobs.collectAsStateWithLifecycle()
    val activeFtpConnection by viewModel.activeFtpConnection.collectAsStateWithLifecycle()
    val remoteCurrentPath by viewModel.remoteCurrentPath.collectAsStateWithLifecycle()
    val remoteFiles by viewModel.remoteFiles.collectAsStateWithLifecycle()

    val activeExcelData by viewModel.activeExcelData.collectAsStateWithLifecycle()
    val activeEditingFile by viewModel.activeEditingFile.collectAsStateWithLifecycle()
    val activeEditingRemoteItem by viewModel.activeEditingRemoteItem.collectAsStateWithLifecycle()
    val activeTextContent by viewModel.activeTextContent.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val selectedFilePaths by viewModel.selectedFilePaths.collectAsStateWithLifecycle()

    // Dialog state handlers
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showEncryptDialogForFile by remember { mutableStateOf<FileItem?>(null) }
    var showDecryptDialogForFile by remember { mutableStateOf<FileItem?>(null) }
    var showAddFtpDialog by remember { mutableStateOf(false) }
    var showAddSyncJobDialog by remember { mutableStateOf(false) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showDetailsDialogForFile by remember { mutableStateOf<FileItem?>(null) }
    var renameFileTarget by remember { mutableStateOf<FileItem?>(null) }

    val baseDir = remember(currentDir) {
        val app = viewModel.getApplication<android.app.Application>()
        com.example.data.file.LocalFileManager.getDefaultBaseDir(app)
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar("خطأ: ${msg.message}")
            }
            viewModel.clearUiMessage()
        }
    }

    // Handle Back Press when in sub-directories or editors
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        if (currentScreen == AppScreen.EXCEL_EDITOR || currentScreen == AppScreen.TEXT_EDITOR) {
            currentScreen = AppScreen.FILES
        } else if (currentScreen == AppScreen.FILES) {
            val handled = viewModel.navigateUp()
            if (!handled) {
                currentScreen = AppScreen.HOME
            }
        } else {
            currentScreen = AppScreen.HOME
        }
    }

    // Full screen editors check
    if (currentScreen == AppScreen.EXCEL_EDITOR) {
        ExcelViewerScreen(
            data = activeExcelData,
            fileName = activeEditingRemoteItem?.name ?: activeEditingFile?.name ?: "جدول بيانات",
            isCloudEdit = activeEditingRemoteItem != null,
            onCellEdit = { r, c, v, f -> viewModel.updateExcelCell(r, c, v, f) },
            onAddRow = { viewModel.addExcelRow() },
            onAddColumn = { colName -> viewModel.addExcelColumn(colName) },
            onSave = { viewModel.saveExcelFile() },
            onBack = { currentScreen = AppScreen.FILES }
        )
        return
    }

    if (currentScreen == AppScreen.TEXT_EDITOR) {
        TextEditorScreen(
            content = activeTextContent,
            fileName = activeEditingRemoteItem?.name ?: activeEditingFile?.name ?: "ملف نصي",
            isCloudEdit = activeEditingRemoteItem != null,
            onContentChange = { viewModel.updateTextContent(it) },
            onSave = { viewModel.saveTextFile() },
            onBack = { currentScreen = AppScreen.FILES }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مدير الملفات - File Master",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "FTP Cloud • Excel Engine • AES-256",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFiles(); viewModel.refreshStorageStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { currentScreen = AppScreen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية") },
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.FILES,
                    onClick = { currentScreen = AppScreen.FILES },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "الملفات") },
                    label = { Text("الملفات") },
                    modifier = Modifier.testTag("nav_files")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.FTP,
                    onClick = { currentScreen = AppScreen.FTP },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "FTP Cloud", tint = FtpPurple) },
                    label = { Text("FTP Cloud") },
                    modifier = Modifier.testTag("nav_ftp")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.VAULT,
                    onClick = { currentScreen = AppScreen.VAULT },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "الخزنة", tint = VaultAmber) },
                    label = { Text("الخزنة") },
                    modifier = Modifier.testTag("nav_vault")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    storageStats = storageStats,
                    recentFiles = fileItems,
                    onOpenCategory = { cat ->
                        currentScreen = AppScreen.FILES
                    },
                    onOpenFile = { item ->
                        if (item.isDirectory) {
                            viewModel.navigateToDir(File(item.path))
                            currentScreen = AppScreen.FILES
                        } else if (item.fileType == FileType.EXCEL) {
                            viewModel.openExcelFile(File(item.path))
                            currentScreen = AppScreen.EXCEL_EDITOR
                        } else if (item.fileType == FileType.TEXT) {
                            viewModel.openTextFile(File(item.path))
                            currentScreen = AppScreen.TEXT_EDITOR
                        } else if (item.isEncrypted) {
                            showDecryptDialogForFile = item
                        }
                    },
                    onOpenExcel = { item ->
                        viewModel.openExcelFile(File(item.path))
                        currentScreen = AppScreen.EXCEL_EDITOR
                    },
                    onOpenText = { item ->
                        viewModel.openTextFile(File(item.path))
                        currentScreen = AppScreen.TEXT_EDITOR
                    },
                    onEncrypt = { item -> showEncryptDialogForFile = item },
                    onDecrypt = { item -> showDecryptDialogForFile = item },
                    onUploadFtp = { item ->
                        currentScreen = AppScreen.FTP
                        if (ftpConnections.isNotEmpty()) {
                            viewModel.uploadToFtp(item, remoteCurrentPath)
                        } else {
                            showAddFtpDialog = true
                        }
                    },
                    onRename = { item -> renameFileTarget = item },
                    onDelete = { item -> viewModel.deleteFile(item) },
                    onCreateFolderClick = { showCreateFolderDialog = true },
                    onCreateFileClick = { showCreateFileDialog = true }
                )

                AppScreen.FILES -> FileBrowserScreen(
                    currentDir = currentDir,
                    baseDir = baseDir,
                    files = fileItems,
                    searchQuery = searchQuery,
                    isGridView = isGridView,
                    selectedFilePaths = selectedFilePaths,
                    onToggleFileSelection = { path -> viewModel.toggleFileSelection(path) },
                    onClearFileSelection = { viewModel.clearFileSelection() },
                    onDeleteSelected = { viewModel.deleteSelectedFiles() },
                    onZipSelected = { showZipDialog = true },
                    onUnzipFile = { item -> viewModel.unzipFile(item) },
                    onShowDetails = { item -> showDetailsDialogForFile = item },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onToggleViewMode = { viewModel.toggleViewMode() },
                    onNavigateToDir = { dir -> viewModel.navigateToDir(dir) },
                    onNavigateUp = { viewModel.navigateUp() },
                    onOpenFile = { item ->
                        if (item.isDirectory) {
                            viewModel.navigateToDir(File(item.path))
                        } else if (item.fileType == FileType.EXCEL) {
                            viewModel.openExcelFile(File(item.path))
                            currentScreen = AppScreen.EXCEL_EDITOR
                        } else if (item.fileType == FileType.TEXT) {
                            viewModel.openTextFile(File(item.path))
                            currentScreen = AppScreen.TEXT_EDITOR
                        } else if (item.isEncrypted) {
                            showDecryptDialogForFile = item
                        } else if (item.fileType == FileType.ARCHIVE) {
                            viewModel.unzipFile(item)
                        } else {
                            showDetailsDialogForFile = item
                        }
                    },
                    onOpenExcel = { item ->
                        viewModel.openExcelFile(File(item.path))
                        currentScreen = AppScreen.EXCEL_EDITOR
                    },
                    onOpenText = { item ->
                        viewModel.openTextFile(File(item.path))
                        currentScreen = AppScreen.TEXT_EDITOR
                    },
                    onEncrypt = { item -> showEncryptDialogForFile = item },
                    onDecrypt = { item -> showDecryptDialogForFile = item },
                    onUploadFtp = { item ->
                        currentScreen = AppScreen.FTP
                        if (ftpConnections.isNotEmpty()) {
                            viewModel.uploadToFtp(item, remoteCurrentPath)
                        } else {
                            showAddFtpDialog = true
                        }
                    },
                    onRename = { item -> renameFileTarget = item },
                    onDelete = { item -> viewModel.deleteFile(item) },
                    onCreateFolderClick = { showCreateFolderDialog = true },
                    onCreateFileClick = { showCreateFileDialog = true }
                )

                AppScreen.FTP -> FtpScreen(
                    connections = ftpConnections,
                    activeConnection = activeFtpConnection,
                    remoteCurrentPath = remoteCurrentPath,
                    remoteFiles = remoteFiles,
                    syncJobs = syncJobs,
                    isLoading = isLoading,
                    onAddConnectionClick = { showAddFtpDialog = true },
                    onDeleteConnection = { conn -> viewModel.deleteFtpConnection(conn) },
                    onTestConnection = { conn -> viewModel.testFtpConnection(conn) },
                    onSelectConnection = { conn -> viewModel.selectFtpConnection(conn) },
                    onNavigateRemoteDir = { path -> viewModel.navigateRemoteDir(path) },
                    onDownloadFile = { item -> viewModel.downloadFromFtp(item) },
                    onCloudEditFile = { item ->
                        viewModel.openRemoteFileForCloudEdit(item)
                        if (item.fileType == FileType.EXCEL) {
                            currentScreen = AppScreen.EXCEL_EDITOR
                        } else {
                            currentScreen = AppScreen.TEXT_EDITOR
                        }
                    },
                    onAddSyncJobClick = { showAddSyncJobDialog = true },
                    onRunSyncJob = { job -> viewModel.runSyncJob(job) },
                    onDeleteSyncJob = { job -> viewModel.deleteSyncJob(job) }
                )

                AppScreen.VAULT -> VaultScreen(
                    encryptedFiles = fileItems.filter { it.isEncrypted || it.fileType == FileType.ENCRYPTED },
                    onDecryptFile = { item -> showDecryptDialogForFile = item },
                    onDeleteFile = { item -> viewModel.deleteFile(item) },
                    onEncryptNewClick = {
                        currentScreen = AppScreen.FILES
                    }
                )

                else -> {}
            }

            if (isLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Active Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                viewModel.createFolder(folderName)
                showCreateFolderDialog = false
            }
        )
    }

    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { fileName, fileType ->
                viewModel.createFile(fileName)
                showCreateFileDialog = false
                if (fileType == "EXCEL") {
                    viewModel.openExcelFile(File(currentDir, fileName))
                    currentScreen = AppScreen.EXCEL_EDITOR
                } else if (fileType == "TEXT") {
                    viewModel.openTextFile(File(currentDir, fileName))
                    currentScreen = AppScreen.TEXT_EDITOR
                }
            }
        )
    }

    showEncryptDialogForFile?.let { item ->
        EncryptDialog(
            fileName = item.name,
            onDismiss = { showEncryptDialogForFile = null },
            onConfirm = { password ->
                viewModel.encryptFile(item, password)
                showEncryptDialogForFile = null
            }
        )
    }

    showDecryptDialogForFile?.let { item ->
        DecryptDialog(
            fileName = item.name,
            onDismiss = { showDecryptDialogForFile = null },
            onConfirm = { password ->
                viewModel.decryptFile(item, password)
                showDecryptDialogForFile = null
            }
        )
    }

    if (showAddFtpDialog) {
        AddFtpDialog(
            onDismiss = { showAddFtpDialog = false },
            onConfirm = { name, host, port, user, pass, passive ->
                viewModel.addFtpConnection(name, host, port, user, pass, passive)
                showAddFtpDialog = false
            }
        )
    }

    if (showAddSyncJobDialog) {
        AddSyncJobDialog(
            connections = ftpConnections,
            localCurrentPath = currentDir.absolutePath,
            onDismiss = { showAddSyncJobDialog = false },
            onConfirm = { connId, localP, remoteP, dir, isAuto ->
                viewModel.addSyncJob(connId, localP, remoteP, dir, isAuto)
                showAddSyncJobDialog = false
            }
        )
    }

    if (showZipDialog) {
        CreateZipDialog(
            selectedCount = selectedFilePaths.size,
            onDismiss = { showZipDialog = false },
            onConfirm = { zipName ->
                viewModel.zipSelectedFiles(zipName)
                showZipDialog = false
            }
        )
    }

    showDetailsDialogForFile?.let { item ->
        FileDetailsDialog(
            fileItem = item,
            onDismiss = { showDetailsDialogForFile = null }
        )
    }

    renameFileTarget?.let { item ->
        var newName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { renameFileTarget = null },
            title = { Text("إعادة تسمية الملف") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFile(item, newName.trim())
                            renameFileTarget = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameFileTarget = null }) { Text("إلغاء") }
            }
        )
    }
}
