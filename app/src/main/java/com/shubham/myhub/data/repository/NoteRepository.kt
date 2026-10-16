package com.shubham.myhub.data.repository

import com.shubham.myhub.data.local.dao.NoteDao
import com.shubham.myhub.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class NoteRepository(
    private val noteDao: NoteDao
) {
    /**
     * Notes for the grid: the normal ones, or only hidden ones when [hidden] is true.
     * Locked notes come back with empty content.
     */
    fun observeNotePreviews(query: String, hidden: Boolean): Flow<List<NoteEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            noteDao.getNotePreviews(hidden)
        } else {
            noteDao.searchNotePreviews(trimmed, hidden)
        }
    }

    suspend fun setHidden(id: Long, hidden: Boolean) {
        noteDao.updateHidden(id, hidden)
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        noteDao.updatePinned(id, pinned)
    }

    /** Full note including content. Only call after the user is allowed to see it. */
    suspend fun getNote(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun setLocked(id: Long, locked: Boolean) {
        noteDao.updateLockStatus(id, locked)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    /** Constant-time comparison so response time doesn't leak how many digits matched. */
    fun verifyPassword(input: String): Boolean =
        MessageDigest.isEqual(
            input.toByteArray(Charsets.UTF_8),
            HARDCODED_LOCK_PASSWORD.toByteArray(Charsets.UTF_8)
        )

    companion object {
        const val HARDCODED_LOCK_PASSWORD = "986727"
    }
}
