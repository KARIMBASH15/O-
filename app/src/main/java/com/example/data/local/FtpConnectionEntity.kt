package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ftp_connections")
data class FtpConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 21,
    val username: String,
    val passwordEncrypted: String,
    val remotePath: String = "/",
    val isPassiveMode: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
)
