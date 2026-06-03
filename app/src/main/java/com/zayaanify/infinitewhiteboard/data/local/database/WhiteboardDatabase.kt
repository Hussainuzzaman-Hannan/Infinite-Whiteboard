package com.zayaanify.infinitewhiteboard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zayaanify.infinitewhiteboard.domain.model.BoardPageEntity

@Database(
    entities = [BoardPageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WhiteboardDatabase : RoomDatabase() {

    abstract fun boardPageDao(): BoardPageDao

    companion object {
        @Volatile
        private var INSTANCE: WhiteboardDatabase? = null

        fun getDatabase(context: Context): WhiteboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhiteboardDatabase::class.java,
                    "whiteboard_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}