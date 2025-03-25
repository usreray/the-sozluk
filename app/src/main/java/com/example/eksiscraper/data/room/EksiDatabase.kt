package com.example.eksiscraper.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedTopicEntity::class], version = 2, exportSchema = false)
abstract class EksiDatabase : RoomDatabase() {
    abstract fun savedTopicDao(): SavedTopicDao
    
    companion object {
        @Volatile
        private var INSTANCE: EksiDatabase? = null
        
        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table without the totalPages column
                database.execSQL(
                    "CREATE TABLE saved_topics_new (" +
                    "title TEXT PRIMARY KEY NOT NULL, " +
                    "url TEXT NOT NULL, " +
                    "commentCount INTEGER NOT NULL, " +
                    "redirectedUrl TEXT NOT NULL)"
                )
                
                // Copy the data from the old table to the new table
                database.execSQL(
                    "INSERT INTO saved_topics_new (title, url, commentCount, redirectedUrl) " +
                    "SELECT title, url, commentCount, redirectedUrl FROM saved_topics"
                )
                
                // Remove the old table
                database.execSQL("DROP TABLE saved_topics")
                
                // Rename the new table to the old table's name
                database.execSQL("ALTER TABLE saved_topics_new RENAME TO saved_topics")
            }
        }
        
        fun getDatabase(context: Context): EksiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EksiDatabase::class.java,
                    "eksi_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 