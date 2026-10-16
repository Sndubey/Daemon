package com.shubham.myhub.data.repository

import com.shubham.myhub.data.local.dao.TimetableDao
import com.shubham.myhub.data.local.entity.TimetableEntity
import com.shubham.myhub.data.local.entity.TimetableEntryEntity
import com.shubham.myhub.data.local.entity.TimetableWithEntries
import kotlinx.coroutines.flow.Flow

class TimetableRepository(
    private val timetableDao: TimetableDao
) {
    val timetables: Flow<List<TimetableWithEntries>> = timetableDao.observeTimetables()

    suspend fun createTimetable(title: String) {
        if (title.isBlank()) return
        timetableDao.insertTimetable(TimetableEntity(title = title.trim()))
    }

    suspend fun renameTimetable(id: Long, title: String) {
        if (title.isBlank()) return
        timetableDao.renameTimetable(id, title.trim())
    }

    /** Deletes the timetable (its entries cascade) and returns everything needed to undo. */
    suspend fun deleteTimetable(id: Long): TimetableWithEntries? {
        val snapshot = timetableDao.getTimetable(id) ?: return null
        timetableDao.deleteTimetable(id)
        return snapshot
    }

    suspend fun restoreTimetable(snapshot: TimetableWithEntries) {
        timetableDao.insertTimetable(snapshot.timetable)
        timetableDao.insertEntries(snapshot.entries)
    }

    suspend fun addEntry(timetableId: Long, dayOfWeek: Int, text: String) {
        if (text.isBlank()) return
        timetableDao.insertEntry(
            TimetableEntryEntity(timetableId = timetableId, dayOfWeek = dayOfWeek, text = text.trim())
        )
    }

    suspend fun updateEntry(id: Long, text: String) {
        if (text.isBlank()) return
        timetableDao.updateEntryText(id, text.trim())
    }

    suspend fun deleteEntry(id: Long) {
        timetableDao.deleteEntry(id)
    }
}
