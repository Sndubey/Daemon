package com.shubham.myhub.ui.timetable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.myhub.data.local.entity.TimetableWithEntries
import com.shubham.myhub.data.repository.TimetableRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimetableViewModel(
    private val repository: TimetableRepository
) : ViewModel() {

    val timetables: StateFlow<List<TimetableWithEntries>> = repository.timetables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Last deleted timetable with its entries, kept so the user can undo. */
    var recentlyDeleted by mutableStateOf<TimetableWithEntries?>(null)
        private set

    fun createTimetable(title: String) {
        viewModelScope.launch { repository.createTimetable(title) }
    }

    fun renameTimetable(id: Long, title: String) {
        viewModelScope.launch { repository.renameTimetable(id, title) }
    }

    fun deleteTimetable(id: Long) {
        viewModelScope.launch {
            recentlyDeleted = repository.deleteTimetable(id)
        }
    }

    fun undoDelete() {
        val snapshot = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { repository.restoreTimetable(snapshot) }
    }

    fun clearRecentlyDeleted() {
        recentlyDeleted = null
    }

    fun addEntry(timetableId: Long, dayOfWeek: Int, text: String) {
        viewModelScope.launch { repository.addEntry(timetableId, dayOfWeek, text) }
    }

    fun updateEntry(id: Long, text: String) {
        viewModelScope.launch { repository.updateEntry(id, text) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repository.deleteEntry(id) }
    }
}
