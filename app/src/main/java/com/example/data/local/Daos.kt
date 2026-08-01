package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FtpConnectionDao {
    @Query("SELECT * FROM ftp_connections ORDER BY lastConnected DESC")
    fun getAllConnections(): Flow<List<FtpConnectionEntity>>

    @Query("SELECT * FROM ftp_connections WHERE id = :id")
    suspend fun getConnectionById(id: Long): FtpConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: FtpConnectionEntity): Long

    @Delete
    suspend fun deleteConnection(connection: FtpConnectionEntity)
}

@Dao
interface SyncJobDao {
    @Query("SELECT * FROM sync_jobs ORDER BY id DESC")
    fun getAllSyncJobs(): Flow<List<SyncJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncJob(job: SyncJobEntity): Long

    @Update
    suspend fun updateSyncJob(job: SyncJobEntity)

    @Delete
    suspend fun deleteSyncJob(job: SyncJobEntity)
}

@Dao
interface FileBookmarkDao {
    @Query("SELECT * FROM file_bookmarks ORDER BY addedTime DESC")
    fun getAllBookmarks(): Flow<List<FileBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: FileBookmarkEntity)

    @Query("DELETE FROM file_bookmarks WHERE filePath = :path")
    suspend fun deleteBookmark(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM file_bookmarks WHERE filePath = :path)")
    suspend fun isBookmarked(path: String): Boolean
}
