package com.example.smartnotify

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MpesaDao {
    @Insert
    suspend fun insert(message: MpesaMessage)

    @Update
    suspend fun update(message: MpesaMessage)

    @Query("SELECT * FROM mpesa_messages WHERE status = 'QUEUED' ORDER BY timestamp ASC")
    suspend fun getQueuedMessages(): List<MpesaMessage>

    @Query("SELECT * FROM mpesa_messages WHERE status = 'QUEUED' ORDER BY timestamp DESC")
    fun getQueuedMessagesFlow(): Flow<List<MpesaMessage>>

    @Query("SELECT * FROM mpesa_messages WHERE status = 'SENT' ORDER BY timestamp DESC")
    fun getSentMessagesFlow(): Flow<List<MpesaMessage>>
}
