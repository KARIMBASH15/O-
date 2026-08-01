package com.example.data.file

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class StorageStats(
    val totalSpaceBytes: Long,
    val freeSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val excelCount: Int = 0,
    val textCount: Int = 0,
    val imageCount: Int = 0,
    val encryptedCount: Int = 0
)

object LocalFileManager {

    fun getDefaultBaseDir(context: Context): File {
        val sdCard = Environment.getExternalStorageDirectory()
        if (sdCard != null && sdCard.exists()) {
            return sdCard
        }
        val storageRoot = File("/storage/emulated/0")
        if (storageRoot.exists()) {
            return storageRoot
        }
        val externalDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val dir = externalDocs ?: File(context.filesDir, "Documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun ensureSampleFiles(context: Context): File = withContext(Dispatchers.IO) {
        val baseDir = getDefaultBaseDir(context)
        try {
            val excelDir = File(baseDir, "جدول_المستندات")
            if (!excelDir.exists()) excelDir.mkdirs()

            // 1. Sample Excel / CSV
            val sampleCsv = File(excelDir, "الميزانية_السنوية.csv")
            if (!sampleCsv.exists()) {
                val csvContent = """
                    البند,الربع الأول,الربع الثاني,الربع الثالث,الربع الرابع,الإجمالي
                    المبيعات,15000,18500,21000,26000,80500
                    التسويق,3000,3500,4000,4500,15000
                    التطوير,8000,8000,9000,9000,34000
                    الرواتب,12000,12000,12000,12000,48000
                    الربح الصافي,2000,5000,6000,9500,22500
                """.trimIndent()
                sampleCsv.writeText(csvContent, Charsets.UTF_8)
            }

            val sampleXlsx = File(excelDir, "قائمة_العملاء_والمشتريات.csv")
            if (!sampleXlsx.exists()) {
                val xlsxContent = """
                    اسم العملاء,المنتج,الكمية,سعر الوحدة,المبلغ الكلي,الحالة
                    شركة الأمل,سيرفر cloud,2,1200,2400,مكتمل
                    مؤسسة النور,رخصة برنامج,5,350,1750,مكتمل
                    مكتب التقنية,تجهيزات شبكات,1,850,850,قيد المعالجة
                    مختبر الحلول,استشارات FTP,3,500,1500,مكتمل
                """.trimIndent()
                sampleXlsx.writeText(xlsxContent, Charsets.UTF_8)
            }

            // 2. Sample Text & Code files
            val textDir = File(baseDir, "الملفات_النصية")
            if (!textDir.exists()) textDir.mkdirs()

            val notesFile = File(textDir, "ملاحظات_المشروع.txt")
            if (!notesFile.exists()) {
                notesFile.writeText(
                    """
                    === دليل تطبيق إدارة الملفات والتعديل السحابي ===
                    1. فتح وتعديل ملفات Excel و CSV مباشرة في التطبيق.
                    2. الاتصال بسيرفرات FTP للتصفح والتعديل السحابي المباشر.
                    3. التشفير الآمن للملفات باستخدام خوارزمية AES-256.
                    4. المزامنة التلقائية بين مجلدات الجهاز وسيرفر FTP.
                    """.trimIndent(),
                    Charsets.UTF_8
                )
            }

            val configFile = File(textDir, "إعدادات_المزامنة.json")
            if (!configFile.exists()) {
                configFile.writeText(
                    """
                    {
                      "appName": "FileMaster",
                      "version": "1.0.0",
                      "syncIntervalMinutes": 15,
                      "encryption": "AES-256-CBC",
                      "defaultFtpPort": 21
                    }
                    """.trimIndent(),
                    Charsets.UTF_8
                )
            }

            // 3. Sample Encrypted Vault
            val vaultDir = File(baseDir, "المخزن_المشفر")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val sampleSecret = File(vaultDir, "مستندات_سرية.enc")
            if (!sampleSecret.exists()) {
                val tempSecret = File(vaultDir, "temp_secret.txt")
                tempSecret.writeText("هذا نص سري مشفر بحماية عالية AES-256 لا يمكن فتحه إلا بكلمة المرور 1234", Charsets.UTF_8)
                EncryptionEngine.encryptFile(tempSecret, sampleSecret, "1234")
                tempSecret.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        baseDir
    }

    suspend fun getDirectoryFiles(dir: File): List<FileItem> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val files = dir.listFiles() ?: arrayOf()
        files.map { FileItem.fromFile(it) }
            .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    suspend fun getStorageStats(context: Context): StorageStats = withContext(Dispatchers.IO) {
        val baseDir = getDefaultBaseDir(context)
        val totalSpace = baseDir.totalSpace
        val freeSpace = baseDir.freeSpace
        val usedSpace = (totalSpace - freeSpace).coerceAtLeast(0)

        var excelCount = 0
        var textCount = 0
        var imageCount = 0
        var encCount = 0

        fun scan(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { scan(it) }
            } else {
                val item = FileItem.fromFile(file)
                when (item.fileType) {
                    FileType.EXCEL -> excelCount++
                    FileType.TEXT -> textCount++
                    FileType.IMAGE -> imageCount++
                    FileType.ENCRYPTED -> encCount++
                    else -> {}
                }
            }
        }
        scan(baseDir)

        StorageStats(
            totalSpaceBytes = totalSpace,
            freeSpaceBytes = freeSpace,
            usedSpaceBytes = usedSpace,
            excelCount = excelCount,
            textCount = textCount,
            imageCount = imageCount,
            encryptedCount = encCount
        )
    }

    suspend fun createDirectory(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parentDir, name)
            if (newDir.exists()) return@withContext Result.failure(Exception("المجلد موجود بالفعل"))
            if (newDir.mkdirs()) Result.success(newDir)
            else Result.failure(Exception("فشل إنشاء المجلد"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentDir: File, name: String, initialContent: String = ""): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentDir, name)
            if (newFile.exists()) return@withContext Result.failure(Exception("الملف موجود بالفعل"))
            newFile.writeText(initialContent, Charsets.UTF_8)
            Result.success(newFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFileOrDir(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(file.parentFile, newName)
            if (file.renameTo(target)) Result.success(target)
            else Result.failure(Exception("فشل إعادة التسمية"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFile(src: File, destDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destDir, src.name)
            if (src.isDirectory) {
                src.copyRecursively(destFile, overwrite = true)
            } else {
                src.copyTo(destFile, overwrite = true)
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun zipFiles(sourceFiles: List<File>, zipFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(zipFile))).use { zos ->
                for (file in sourceFiles) {
                    if (file.exists()) {
                        if (file.isDirectory) {
                            file.walkTopDown().forEach { walkFile ->
                                val relPath = file.parentFile?.let { walkFile.relativeTo(it).path } ?: walkFile.name
                                if (walkFile.isFile) {
                                    java.io.FileInputStream(walkFile).use { fis ->
                                        val entry = java.util.zip.ZipEntry(relPath)
                                        zos.putNextEntry(entry)
                                        fis.copyTo(zos)
                                        zos.closeEntry()
                                    }
                                }
                            }
                        } else {
                            java.io.FileInputStream(file).use { fis ->
                                val entry = java.util.zip.ZipEntry(file.name)
                                zos.putNextEntry(entry)
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unzipFile(zipFile: File, destDir: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(java.io.FileInputStream(zipFile))).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        java.io.FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
