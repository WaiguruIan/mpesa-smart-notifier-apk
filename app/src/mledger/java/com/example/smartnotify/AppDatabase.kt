package com.example.smartnotify

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MpesaMessage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mpesaDao(): MpesaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mpesa_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
