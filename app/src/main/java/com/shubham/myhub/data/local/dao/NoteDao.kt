package com.shubham.myhub.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shubham.myhub.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    // List queries never expose the content of locked notes: it is blanked out in SQL,
    // so it is never held in UI state and cannot be matched by search.
    @Query(
        """
        SELECT id, title, CASE WHEN isLocked = 1 THEN '' ELSE content END AS content,
               categoryIcon, isLocked, updatedAtMillis
        FROM notes ORDER BY updatedAtMillis DESC
        """
    )
    fun getNotePreviews(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT id, title, CASE WHEN isLocked = 1 THEN '' ELSE content END AS content,
               categoryIcon, isLocked, updatedAtMillis
        FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR (isLocked = 0 AND content LIKE '%' || :query || '%')
        ORDER BY updatedAtMillis DESC
        """
    )
    fun searchNotePreviews(query: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLockStatus(id: Long, isLocked: Boolean): Int

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
