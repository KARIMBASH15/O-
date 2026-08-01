package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_bookmarks")
data class FileBookmarkEntity(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val isEncrypted: Boolean = false,
    val addedTime: Long = System.currentTimeMillis()
)
