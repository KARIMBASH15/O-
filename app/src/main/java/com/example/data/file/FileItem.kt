package com.example.data.file

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class FileType {
    DIRECTORY,
    EXCEL,      // .xlsx, .xls, .csv
    TEXT,       // .txt, .json, .xml, .md, .log, .html, .py, .kt
    IMAGE,      // .png, .jpg, .jpeg, .webp, .gif, .svg
    AUDIO,      // .mp3, .wav, .aac, .m4a, .ogg
    VIDEO,      // .mp4, .mkv, .webm, .avi
    PDF,        // .pdf
    ARCHIVE,    // .zip, .rar, .7z, .tar, .gz
    ENCRYPTED,  // .enc, .aes
    GENERIC
}

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val fileType: FileType,
    val isEncrypted: Boolean = false,
    val isRemote: Boolean = false,
    val extension: String = ""
) {
    val formattedSize: String
        get() {
            if (isDirectory) return ""
            if (size < 1024) return "$size B"
            val exp = (Math.log(size.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format(Locale.getDefault(), "%.1f %cB", size / Math.pow(1024.0, exp.toDouble()), pre)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    companion object {
        fun fromFile(file: File): FileItem {
            val isDir = file.isDirectory
            val name = file.name
            val ext = if (isDir) "" else file.extension.lowercase(Locale.ROOT)
            val type = detectFileType(ext, isDir)
            val encrypted = type == FileType.ENCRYPTED || name.endsWith(".enc", ignoreCase = true)
            
            return FileItem(
                name = name,
                path = file.absolutePath,
                size = if (isDir) 0L else file.length(),
                lastModified = file.lastModified(),
                isDirectory = isDir,
                fileType = type,
                isEncrypted = encrypted,
                isRemote = false,
                extension = ext
            )
        }

        fun detectFileType(ext: String, isDir: Boolean): FileType {
            if (isDir) return FileType.DIRECTORY
            return when (ext) {
                "xlsx", "xls", "csv" -> FileType.EXCEL
                "txt", "json", "xml", "md", "log", "html", "htm", "css", "js", "kt", "java", "py", "c", "cpp" -> FileType.TEXT
                "png", "jpg", "jpeg", "webp", "gif", "bmp", "svg" -> FileType.IMAGE
                "mp3", "wav", "aac", "m4a", "ogg", "flac" -> FileType.AUDIO
                "mp4", "mkv", "webm", "avi", "mov", "3gp" -> FileType.VIDEO
                "pdf" -> FileType.PDF
                "zip", "rar", "7z", "tar", "gz" -> FileType.ARCHIVE
                "enc", "aes", "locked" -> FileType.ENCRYPTED
                else -> FileType.GENERIC
            }
        }
    }
}
