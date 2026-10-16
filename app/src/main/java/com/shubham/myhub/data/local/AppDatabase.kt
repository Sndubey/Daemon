package com.shubham.myhub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shubham.myhub.data.local.dao.GoalDao
import com.shubham.myhub.data.local.dao.NoteDao
import com.shubham.myhub.data.local.dao.TimetableDao
import com.shubham.myhub.data.local.dao.TodoDao
import com.shubham.myhub.data.local.entity.GoalEntity
import com.shubham.myhub.data.local.entity.NoteEntity
import com.shubham.myhub.data.local.entity.TimetableEntity
import com.shubham.myhub.data.local.entity.TimetableEntryEntity
import com.shubham.myhub.data.local.entity.TodoEntity

@Database(
    entities = [
        TodoEntity::class,
        GoalEntity::class,
        NoteEntity::class,
        TimetableEntity::class,
        TimetableEntryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun todoDao(): TodoDao
    abstract fun goalDao(): GoalDao
    abstract fun noteDao(): NoteDao
    abstract fun timetableDao(): TimetableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v4 adds timetables. Only creates new tables; existing data is untouched. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timetables` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `timetable_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timetableId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, `text` TEXT NOT NULL, " +
                        "`createdAtMillis` INTEGER NOT NULL, FOREIGN KEY(`timetableId`) REFERENCES `timetables`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_timetable_entries_timetableId` ON `timetable_entries` (`timetableId`)"
                )
            }
        }

        /**
         * v5 adds todos.sortOrder for drag-to-reorder. Existing to-dos are numbered newest-first,
         * so the list looks exactly as it did before the update.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE `todos` SET `sortOrder` = (SELECT COUNT(*) FROM `todos` AS t2 " +
                        "WHERE t2.`createdAtMillis` > `todos`.`createdAtMillis` " +
                        "OR (t2.`createdAtMillis` = `todos`.`createdAtMillis` AND t2.`id` > `todos`.`id`))"
                )
            }
        }

        /** v6 adds notes.isHidden. Every existing note starts visible. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isHidden` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v7 adds notes.isPinned. Every existing note starts unpinned. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myhub_main_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
