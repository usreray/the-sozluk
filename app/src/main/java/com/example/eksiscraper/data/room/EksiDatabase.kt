package com.example.eksiscraper.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedTopicEntity::class], version = 1, exportSchema = false)
abstract class EksiDatabase : RoomDatabase() {
    abstract fun savedTopicDao(): SavedTopicDao
    
    companion object {
        @Volatile
        private var INSTANCE: EksiDatabase? = null
        
        fun getDatabase(context: Context): EksiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EksiDatabase::class.java,
                    "eksi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 