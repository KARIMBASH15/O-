package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.file.*
import com.example.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val ftpDao = db.ftpConnectionDao()
    private val syncDao = db.syncJobDao()
    private val bookmarkDao = db.fileBookmarkDao()

    // Base storage path & current browsing path
    private val _currentDir = MutableStateFlow<File>(LocalFileManager.getDefaultBaseDir(application))
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(100, 50, 50))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // FTP Connections & Sync Jobs
    val ftpConnections: StateFlow<List<FtpConnectionEntity>> = ftpDao.getAllConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncJobs: StateFlow<List<SyncJobEntity>> = syncDao.getAllSyncJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected FTP Connection for Cloud Explorer
    private val _activeFtpConnection = MutableStateFlow<FtpConnectionEntity?>(null)
    val activeFtpConnection: StateFlow<FtpConnectionEntity?> = _activeFtpConnection.asStateFlow()

    private val _remoteCurrentPath = MutableStateFlow("/")
    val remoteCurrentPath: StateFlow<String> = _remoteCurrentPath.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<RemoteFileItem>>(emptyList())
    val remoteFiles: StateFlow<List<RemoteFileItem>> = _remoteFiles.asStateFlow()

    // Active Active Editing Session (Excel or Text)
    private val _activeExcelData = MutableStateFlow<ExcelData?>(null)
    val activeExcelData: StateFlow<ExcelData?> = _activeExcelData.asStateFlow()

    private val _activeEditingFile = MutableStateFlow<File?>(null)
    val activeEditingFile: StateFlow<File?> = _activeEditingFile.asStateFlow()

    private val _activeEditingRemoteItem = MutableStateFlow<RemoteFileItem?>(null)
    val activeEditingRemoteItem: StateFlow<RemoteFileItem?> = _activeEditingRemoteItem.asStateFlow()

    private val _activeTextContent = MutableStateFlow("")
    val activeTextContent: StateFlow<String> = _activeTextContent.asStateFlow()

    // Search query & View mode
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Multi-selection state
    private val _selectedFilePaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilePaths: StateFlow<Set<String>> = _selectedFilePaths.asStateFlow()

    fun toggleFileSelection(path: String) {
        val current = _selectedFilePaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedFilePaths.value = current
    }

    fun clearFileSelection() {
        _selectedFilePaths.value = emptySet()
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val paths = _selectedFilePaths.value.toList()
            var count = 0
            for (path in paths) {
                if (LocalFileManager.deleteFileOrDir(File(path))) {
                    count++
                }
            }
            _selectedFilePaths.value = emptySet()
            _uiMessage.value = UiMessage.Success("تم حذف $count ملف/مجلد بنجاح")
            refreshFiles()
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun zipSelectedFiles(zipName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val selectedFiles = _selectedFilePaths.value.map { File(it) }
            val finalZipName = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
            val targetZipFile = File(_currentDir.value, finalZipName)
            val res = LocalFileManager.zipFiles(selectedFiles, targetZipFile)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم ضغط الملفات المحددة إلى '${targetZipFile.name}'")
                _selectedFilePaths.value = emptySet()
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error("فشل عملية الضغط: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    fun unzipFile(fileItem: FileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val zipFile = File(fileItem.path)
            val extractDir = File(_currentDir.value, zipFile.nameWithoutExtension)
            val res = LocalFileManager.unzipFile(zipFile, extractDir)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم فك الضغط بنجاح إلى مجلد '${extractDir.name}'")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error("فشل فك ضغط الملف: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    init {
        viewModelScope.launch {
            _isLoading.value = true
            val baseDir = LocalFileManager.ensureSampleFiles(application)
            _currentDir.value = baseDir
            refreshFiles()
            refreshStorageStats()
            _isLoading.value = false
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun navigateToDir(dir: File) {
        _currentDir.value = dir
        refreshFiles()
    }

    fun navigateUp(): Boolean {
        val current = _currentDir.value
        val parent = current.parentFile
        if (parent != null && parent.canRead() && current.absolutePath != "/" && current.absolutePath != "/storage") {
            _currentDir.value = parent
            refreshFiles()
            return true
        }
        return false
    }

    fun resetToBaseDir() {
        val baseDir = LocalFileManager.getDefaultBaseDir(getApplication())
        _currentDir.value = baseDir
        refreshFiles()
        refreshStorageStats()
    }

    fun refreshFiles() {
        viewModelScope.launch {
            val list = LocalFileManager.getDirectoryFiles(_currentDir.value)
            _fileItems.value = list
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = LocalFileManager.getStorageStats(getApplication())
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val res = LocalFileManager.createDirectory(_currentDir.value, name)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم إنشاء المجلد '$name' بنجاح")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error(res.exceptionOrNull()?.message ?: "فشل إنشاء المجلد")
            }
        }
    }

    fun createFile(name: String, content: String = "") {
        viewModelScope.launch {
            val res = LocalFileManager.createFile(_currentDir.value, name, content)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم إنشاء الملف '$name' بنجاح")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error(res.exceptionOrNull()?.message ?: "فشل إنشاء الملف")
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            val file = File(fileItem.path)
            val deleted = LocalFileManager.deleteFileOrDir(file)
            if (deleted) {
                _uiMessage.value = UiMessage.Success("تم حذف '${fileItem.name}'")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error("فشل حذف الملف")
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            val file = File(fileItem.path)
            val res = LocalFileManager.renameFile(file, newName)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تمت إعادة تسمية الملف إلى '$newName'")
                refreshFiles()
            } else {
                _uiMessage.value = UiMessage.Error(res.exceptionOrNull()?.message ?: "فشل إعادة التسمية")
            }
        }
    }

    // --- Excel Editor ---
    fun openExcelFile(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _activeEditingFile.value = file
            _activeEditingRemoteItem.value = null
            _activeExcelData.value = ExcelEngine.loadExcelOrCsv(file)
            _isLoading.value = false
        }
    }

    fun updateExcelCell(rowIndex: Int, colIndex: Int, value: String, formula: String = "") {
        val currentData = _activeExcelData.value ?: return
        if (rowIndex < currentData.rows.size) {
            val row = currentData.rows[rowIndex]
            while (row.cells.size <= colIndex) {
                row.cells.add(ExcelCell())
            }
            row.cells[colIndex] = ExcelCell(value = value, formula = formula)
            _activeExcelData.value = currentData.copy()
        }
    }

    fun addExcelRow() {
        val currentData = _activeExcelData.value ?: return
        val colCount = maxOf(currentData.headers.size, 5)
        val newRow = ExcelRow(
            rowIndex = currentData.rows.size,
            cells = MutableList(colCount) { ExcelCell() }
        )
        currentData.rows.add(newRow)
        _activeExcelData.value = currentData.copy()
    }

    fun addExcelColumn(headerName: String) {
        val currentData = _activeExcelData.value ?: return
        val newColName = if (headerName.isBlank()) ExcelEngine.getColName(currentData.headers.size) else headerName
        currentData.headers.add(newColName)
        currentData.rows.forEach { row ->
            row.cells.add(ExcelCell())
        }
        _activeExcelData.value = currentData.copy()
    }

    fun saveExcelFile() {
        val data = _activeExcelData.value ?: return
        val localFile = _activeEditingFile.value
        val remoteItem = _activeEditingRemoteItem.value
        val ftpConn = _activeFtpConnection.value

        viewModelScope.launch {
            _isLoading.value = true
            if (localFile != null) {
                val saved = ExcelEngine.saveExcelOrCsv(localFile, data)
                if (saved) {
                    _uiMessage.value = UiMessage.Success("تم حفظ التعديلات على جدول البيانات")
                    refreshFiles()
                    // If cloud editing mode: push back to FTP!
                    if (remoteItem != null && ftpConn != null) {
                        val uploadRes = FtpService.uploadLocalFile(ftpConn, localFile, remoteItem.remotePath)
                        if (uploadRes.isSuccess) {
                            _uiMessage.value = UiMessage.Success("تم حفظ وتحديث الملف سحابياً على السيرفر FTP بنجاح!")
                        } else {
                            _uiMessage.value = UiMessage.Error("تم الحفظ محلياً لكن فشل الرفع إلى FTP: ${uploadRes.exceptionOrNull()?.message}")
                        }
                    }
                } else {
                    _uiMessage.value = UiMessage.Error("فشل حفظ الملف")
                }
            }
            _isLoading.value = false
        }
    }

    // --- Text Editor ---
    fun openTextFile(file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _activeEditingFile.value = file
            _activeEditingRemoteItem.value = null
            _activeTextContent.value = file.readText(Charsets.UTF_8)
            _isLoading.value = false
        }
    }

    fun updateTextContent(content: String) {
        _activeTextContent.value = content
    }

    fun saveTextFile() {
        val file = _activeEditingFile.value ?: return
        val remoteItem = _activeEditingRemoteItem.value
        val ftpConn = _activeFtpConnection.value

        viewModelScope.launch {
            _isLoading.value = true
            try {
                file.writeText(_activeTextContent.value, Charsets.UTF_8)
                _uiMessage.value = UiMessage.Success("تم حفظ الملف النصي بنجاح")
                refreshFiles()

                if (remoteItem != null && ftpConn != null) {
                    val uploadRes = FtpService.uploadLocalFile(ftpConn, file, remoteItem.remotePath)
                    if (uploadRes.isSuccess) {
                        _uiMessage.value = UiMessage.Success("تم التعديل السحابي وحفظ الملف على FTP بنجاح!")
                    } else {
                        _uiMessage.value = UiMessage.Error("تم الحفظ محلياً لكن فشل الرفع للسيرفر FTP")
                    }
                }
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Error("فشل حفظ الملف النصي")
            }
            _isLoading.value = false
        }
    }

    // --- Encryption / Decryption Vault ---
    fun encryptFile(fileItem: FileItem, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val srcFile = File(fileItem.path)
            val outFile = File(srcFile.parentFile, "${srcFile.nameWithoutExtension}.enc")
            val res = EncryptionEngine.encryptFile(srcFile, outFile, password)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم تشفير الملف بنجاح وتخزينه باسم '${outFile.name}' (AES-256)")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error("فشل التشفير: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    fun decryptFile(fileItem: FileItem, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val srcFile = File(fileItem.path)
            val outName = if (srcFile.name.endsWith(".enc")) srcFile.name.removeSuffix(".enc") else "${srcFile.nameWithoutExtension}_decrypted.txt"
            val outFile = File(srcFile.parentFile, outName)
            val res = EncryptionEngine.decryptFile(srcFile, outFile, password)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم فك تشفير الملف بنجاح باسم '${outFile.name}'")
                refreshFiles()
                refreshStorageStats()
            } else {
                _uiMessage.value = UiMessage.Error("خطأ فك التشفير: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    // --- FTP Management ---
    fun addFtpConnection(name: String, host: String, port: Int, user: String, pass: String, passive: Boolean) {
        viewModelScope.launch {
            val entity = FtpConnectionEntity(
                name = name,
                host = host,
                port = port,
                username = user,
                passwordEncrypted = pass,
                isPassiveMode = passive
            )
            ftpDao.insertConnection(entity)
            _uiMessage.value = UiMessage.Success("تم إضافة سيرفر FTP: '$name'")
        }
    }

    fun deleteFtpConnection(connection: FtpConnectionEntity) {
        viewModelScope.launch {
            ftpDao.deleteConnection(connection)
            if (_activeFtpConnection.value?.id == connection.id) {
                _activeFtpConnection.value = null
                _remoteFiles.value = emptyList()
            }
            _uiMessage.value = UiMessage.Success("تم حذف الاتصال بالسيرفر")
        }
    }

    fun testFtpConnection(connection: FtpConnectionEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = FtpService.testConnection(connection)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success(res.getOrDefault("تم الاتصال بنجاح"))
            } else {
                _uiMessage.value = UiMessage.Error(res.exceptionOrNull()?.message ?: "فشل الاتصال")
            }
            _isLoading.value = false
        }
    }

    fun selectFtpConnection(connection: FtpConnectionEntity) {
        _activeFtpConnection.value = connection
        _remoteCurrentPath.value = connection.remotePath
        fetchRemoteFiles()
    }

    fun fetchRemoteFiles() {
        val conn = _activeFtpConnection.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = FtpService.listRemoteFiles(conn, _remoteCurrentPath.value)
            if (res.isSuccess) {
                _remoteFiles.value = res.getOrDefault(emptyList())
            } else {
                _uiMessage.value = UiMessage.Error("فشل استعراض ملفات السيرفر: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    fun navigateRemoteDir(remotePath: String) {
        _remoteCurrentPath.value = remotePath
        fetchRemoteFiles()
    }

    fun uploadToFtp(fileItem: FileItem, remoteDirPath: String) {
        val conn = _activeFtpConnection.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val localFile = File(fileItem.path)
            val remotePath = "$remoteDirPath/${localFile.name}".replace("//", "/")
            val res = FtpService.uploadLocalFile(conn, localFile, remotePath)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم رفع '${localFile.name}' إلى السيرفر FTP")
                fetchRemoteFiles()
            } else {
                _uiMessage.value = UiMessage.Error("فشل الرفع: ${res.exceptionOrNull()?.message}")
            }
            _isLoading.value = false
        }
    }

    fun downloadFromFtp(remoteItem: RemoteFileItem) {
        val conn = _activeFtpConnection.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val destFile = File(_currentDir.value, remoteItem.name)
            val res = FtpService.downloadRemoteFile(conn, remoteItem.remotePath, destFile)
            if (res.isSuccess) {
                _uiMessage.value = UiMessage.Success("تم تنزيل '${remoteItem.name}' إلى مجلد الجهاز")
                refreshFiles()
            } else {
                _uiMessage.value = UiMessage.Error("فشل التنزيل من السيرفر")
            }
            _isLoading.value = false
        }
    }

    fun openRemoteFileForCloudEdit(remoteItem: RemoteFileItem) {
        val conn = _activeFtpConnection.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val tempDir = File(getApplication<Application>().cacheDir, "cloud_edit")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, remoteItem.name)

            val downloadRes = FtpService.downloadRemoteFile(conn, remoteItem.remotePath, tempFile)
            if (downloadRes.isSuccess) {
                _activeEditingRemoteItem.value = remoteItem
                _activeEditingFile.value = tempFile
                if (remoteItem.fileType == FileType.EXCEL) {
                    _activeExcelData.value = ExcelEngine.loadExcelOrCsv(tempFile)
                } else if (remoteItem.fileType == FileType.TEXT) {
                    _activeTextContent.value = tempFile.readText(Charsets.UTF_8)
                }
            } else {
                _uiMessage.value = UiMessage.Error("فشل تنزيل الملف للتعديل السحابي")
            }
            _isLoading.value = false
        }
    }

    // --- Sync Jobs ---
    fun addSyncJob(connectionId: Long, localPath: String, remotePath: String, direction: String, isAuto: Boolean) {
        viewModelScope.launch {
            val job = SyncJobEntity(
                ftpConnectionId = connectionId,
                localPath = localPath,
                remotePath = remotePath,
                syncDirection = direction,
                isAutoSync = isAuto
            )
            syncDao.insertSyncJob(job)
            _uiMessage.value = UiMessage.Success("تم إضافة مهمة المزامنة السحابية")
        }
    }

    fun runSyncJob(job: SyncJobEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            val conn = ftpDao.getConnectionById(job.ftpConnectionId)
            if (conn == null) {
                _uiMessage.value = UiMessage.Error("سيرفر FTP مرتبط بهذه المهمة غير موجود")
                _isLoading.value = false
                return@launch
            }

            val updatedJob = job.copy(status = "SYNCING", lastSyncLog = "جاري تنفيذ المزامنة...")
            syncDao.updateSyncJob(updatedJob)

            val res = SyncEngine.executeSync(conn, job)
            if (res.isSuccess) {
                val log = res.getOrDefault("اكتملت المزامنة بنجاح.")
                syncDao.updateSyncJob(job.copy(status = "SUCCESS", lastSyncTime = System.currentTimeMillis(), lastSyncLog = log))
                _uiMessage.value = UiMessage.Success("اكتملت مزامنة البيانات بنجاح!")
                refreshFiles()
            } else {
                val errLog = res.exceptionOrNull()?.message ?: "فشلت المزامنة"
                syncDao.updateSyncJob(job.copy(status = "FAILED", lastSyncTime = System.currentTimeMillis(), lastSyncLog = errLog))
                _uiMessage.value = UiMessage.Error("فشلت مهمة المزامنة")
            }
            _isLoading.value = false
        }
    }

    fun deleteSyncJob(job: SyncJobEntity) {
        viewModelScope.launch {
            syncDao.deleteSyncJob(job)
            _uiMessage.value = UiMessage.Success("تم حذف مهمة المزامنة")
        }
    }
}
