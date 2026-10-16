package com.shubham.myhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shubham.myhub.data.local.entity.TimetableEntity
import com.shubham.myhub.data.local.entity.TimetableEntryEntity
import com.shubham.myhub.data.local.entity.TimetableWithEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Transaction
    @Query("SELECT * FROM timetables ORDER BY createdAtMillis ASC")
    fun observeTimetables(): Flow<List<TimetableWithEntries>>

    @Transaction
    @Query("SELECT * FROM timetables WHERE id = :id")
    suspend fun getTimetable(id: Long): TimetableWithEntries?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(timetable: TimetableEntity): Long

    @Query("UPDATE timetables SET title = :title WHERE id = :id")
    suspend fun renameTimetable(id: Long, title: String): Int

    @Query("DELETE FROM timetables WHERE id = :id")
    suspend fun deleteTimetable(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntryEntity>): List<Long>

    @Query("UPDATE timetable_entries SET text = :text WHERE id = :id")
    suspend fun updateEntryText(id: Long, text: String): Int

    @Query("DELETE FROM timetable_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long): Int
}
