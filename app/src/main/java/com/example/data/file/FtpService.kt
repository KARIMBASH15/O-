package com.example.data.file

import com.example.data.local.FtpConnectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class RemoteFileItem(
    val name: String,
    val remotePath: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val fileType: FileType,
    val connectionId: Long
)

object FtpService {

    suspend fun testConnection(connection: FtpConnectionEntity): Result<String> = withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connectTimeout = 10000
            client.connect(connection.host, connection.port)
            val loginSuccess = client.login(connection.username, connection.passwordEncrypted)
            if (!loginSuccess) {
                return@withContext Result.failure(Exception("فشل تسجيل الدخول. يرجى التحقق من اسم المستخدم وكلمة المرور."))
            }
            if (connection.isPassiveMode) {
                client.enterLocalPassiveMode()
            }
            val replyCode = client.replyCode
            client.logout()
            client.disconnect()
            Result.success("تم الاتصال بالسيرفر FTP بنجاح! (رمز الاستجابة: $replyCode)")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("فشل الاتصال بسيرفر FTP: ${e.localizedMessage}"))
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (ignored: Exception) {}
            }
        }
    }

    suspend fun listRemoteFiles(connection: FtpConnectionEntity, path: String): Result<List<RemoteFileItem>> = withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connectTimeout = 10000
            client.connect(connection.host, connection.port)
            if (!client.login(connection.username, connection.passwordEncrypted)) {
                return@withContext Result.failure(Exception("فشل تسجيل الدخول"))
            }
            if (connection.isPassiveMode) {
                client.enterLocalPassiveMode()
            }
            client.setFileType(FTP.BINARY_FILE_TYPE)

            val currentDir = if (path.isBlank()) "/" else path
            client.changeWorkingDirectory(currentDir)

            val ftpFiles = client.listFiles() ?: arrayOf()
            val list = ftpFiles.filter { it.name != "." && it.name != ".." }.map { ftpFile ->
                val isDir = ftpFile.isDirectory
                val name = ftpFile.name
                val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
                val fullPath = if (currentDir.endsWith("/")) "$currentDir$name" else "$currentDir/$name"

                RemoteFileItem(
                    name = name,
                    remotePath = fullPath,
                    size = if (isDir) 0L else ftpFile.size,
                    lastModified = ftpFile.timestamp?.timeInMillis ?: System.currentTimeMillis(),
                    isDirectory = isDir,
                    fileType = FileItem.detectFileType(ext, isDir),
                    connectionId = connection.id
                )
            }.sortedWith(compareByDescending<RemoteFileItem> { it.isDirectory }.thenBy { it.name.lowercase() })

            client.logout()
            client.disconnect()
            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (ignored: Exception) {}
            }
        }
    }

    suspend fun downloadRemoteFile(
        connection: FtpConnectionEntity,
        remotePath: String,
        localDestination: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connectTimeout = 10000
            client.connect(connection.host, connection.port)
            if (!client.login(connection.username, connection.passwordEncrypted)) {
                return@withContext Result.failure(Exception("فشل تسجيل الدخول"))
            }
            if (connection.isPassiveMode) {
                client.enterLocalPassiveMode()
            }
            client.setFileType(FTP.BINARY_FILE_TYPE)

            localDestination.parentFile?.mkdirs()
            FileOutputStream(localDestination).use { fos ->
                val success = client.retrieveFile(remotePath, fos)
                if (!success) {
                    return@withContext Result.failure(Exception("فشل تنزيل الملف من السيرفر FTP"))
                }
            }

            client.logout()
            client.disconnect()
            Result.success(localDestination)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (ignored: Exception) {}
            }
        }
    }

    suspend fun uploadLocalFile(
        connection: FtpConnectionEntity,
        localFile: File,
        remotePath: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connectTimeout = 10000
            client.connect(connection.host, connection.port)
            if (!client.login(connection.username, connection.passwordEncrypted)) {
                return@withContext Result.failure(Exception("فشل تسجيل الدخول"))
            }
            if (connection.isPassiveMode) {
                client.enterLocalPassiveMode()
            }
            client.setFileType(FTP.BINARY_FILE_TYPE)

            val dirPath = remotePath.substringBeforeLast('/', "/")
            val fileName = remotePath.substringAfterLast('/')

            if (dirPath.isNotEmpty()) {
                client.makeDirectory(dirPath)
                client.changeWorkingDirectory(dirPath)
            }

            FileInputStream(localFile).use { fis ->
                val success = client.storeFile(fileName, fis)
                if (!success) {
                    return@withContext Result.failure(Exception("فشل رفع الملف إلى السيرفر FTP"))
                }
            }

            client.logout()
            client.disconnect()
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (ignored: Exception) {}
            }
        }
    }
}
