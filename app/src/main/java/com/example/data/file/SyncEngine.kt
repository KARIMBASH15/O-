package com.example.data.file

import com.example.data.local.FtpConnectionEntity
import com.example.data.local.SyncJobEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SyncEngine {

    suspend fun executeSync(
        connection: FtpConnectionEntity,
        job: SyncJobEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        val localDir = File(job.localPath)
        if (!localDir.exists()) {
            localDir.mkdirs()
        }

        val logBuilder = StringBuilder()
        logBuilder.append("بدء المزامنة [${job.syncDirection}]: ${job.localPath} <-> ${job.remotePath}\n")

        try {
            when (job.syncDirection) {
                "UPLOAD" -> {
                    val localFiles = localDir.listFiles()?.filter { it.isFile } ?: emptyList()
                    var uploadedCount = 0
                    localFiles.forEach { file ->
                        val targetRemotePath = "${job.remotePath}/${file.name}".replace("//", "/")
                        val res = FtpService.uploadLocalFile(connection, file, targetRemotePath)
                        if (res.isSuccess) {
                            uploadedCount++
                            logBuilder.append("✓ تم رفع: ${file.name}\n")
                        } else {
                            logBuilder.append("✗ فشل رفع: ${file.name}\n")
                        }
                    }
                    logBuilder.append("اكتمل الرفع: $uploadedCount ملف.\n")
                }
                "DOWNLOAD" -> {
                    val remoteFilesRes = FtpService.listRemoteFiles(connection, job.remotePath)
                    if (remoteFilesRes.isSuccess) {
                        val remoteFiles = remoteFilesRes.getOrDefault(emptyList()).filter { !it.isDirectory }
                        var downloadedCount = 0
                        remoteFiles.forEach { remoteFile ->
                            val localTargetFile = File(localDir, remoteFile.name)
                            val res = FtpService.downloadRemoteFile(connection, remoteFile.remotePath, localTargetFile)
                            if (res.isSuccess) {
                                downloadedCount++
                                logBuilder.append("✓ تم تنزيل: ${remoteFile.name}\n")
                            } else {
                                logBuilder.append("✗ فشل تنزيل: ${remoteFile.name}\n")
                            }
                        }
                        logBuilder.append("اكتمل التنزيل: $downloadedCount ملف.\n")
                    } else {
                        logBuilder.append("✗ فشل قراءة الملفات من السيرفر.\n")
                    }
                }
                else -> { // BIDIRECTIONAL
                    val localFiles = localDir.listFiles()?.filter { it.isFile } ?: emptyList()
                    localFiles.forEach { file ->
                        val targetRemotePath = "${job.remotePath}/${file.name}".replace("//", "/")
                        FtpService.uploadLocalFile(connection, file, targetRemotePath)
                        logBuilder.append("✓ مزامنة ثنائية رفع: ${file.name}\n")
                    }
                    val remoteFilesRes = FtpService.listRemoteFiles(connection, job.remotePath)
                    remoteFilesRes.getOrNull()?.filter { !it.isDirectory }?.forEach { remoteFile ->
                        val localTargetFile = File(localDir, remoteFile.name)
                        if (!localTargetFile.exists()) {
                            FtpService.downloadRemoteFile(connection, remoteFile.remotePath, localTargetFile)
                            logBuilder.append("✓ مزامنة ثنائية تنزيل: ${remoteFile.name}\n")
                        }
                    }
                    logBuilder.append("اكتملت المزامنة الثنائية بنجاح.\n")
                }
            }
            Result.success(logBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            logBuilder.append("خطأ أثناء المزامنة: ${e.localizedMessage}\n")
            Result.failure(Exception(logBuilder.toString()))
        }
    }
}
