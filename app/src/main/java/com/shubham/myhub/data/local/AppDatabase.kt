package com.shubham.myhub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shubham.myhub.data.local.dao.GoalDao
import com.shubham.myhub.data.local.dao.NoteDao
import com.shubham.myhub.data.local.dao.TodoDao
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.NoteEntity
import com.shubham.myhub.data.local.entity.TodoEntity

@Database(
    entities = [TodoEntity::class, GoalEntity::class, NoteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun goalDao(): GoalDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myhub_main_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
