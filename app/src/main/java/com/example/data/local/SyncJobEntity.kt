package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_jobs")
data class SyncJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ftpConnectionId: Long,
    val localPath: String,
    val remotePath: String,
    val syncDirection: String, // "UPLOAD", "DOWNLOAD", "BIDIRECTIONAL"
    val isAutoSync: Boolean = false,
    val lastSyncTime: Long = 0,
    val status: String = "IDLE", // "IDLE", "SYNCING", "SUCCESS", "FAILED"
    val lastSyncLog: String = ""
)
