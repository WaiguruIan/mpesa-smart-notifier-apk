package com.example.smartnotify

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mpesa_messages")
data class MpesaMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawBody: String,
    val timestamp: Long,
    val status: String // 'QUEUED' or 'SENT'
)
