package com.shubham.myhub.ui.notes

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.myhub.data.local.entity.NoteEntity
import com.shubham.myhub.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class NoteCategory(val emoji: String, val label: String) {
    IDEAS("💡", "Ideas"),
    SHOPPING("🛒", "Shopping"),
    MEETING("📝", "Meeting"),
    PERSONAL("❤️", "Personal"),
    BOOKS("📖", "Books"),
    THOUGHTS("☁️", "Thoughts");

    companion object {
        fun from(key: String): NoteCategory = entries.firstOrNull { it.name == key } ?: THOUGHTS
    }
}

/** The note currently open in the editor. [noteId] is 0 until a new note is first saved. */
data class NoteEditorState(
    val sessionKey: Long,
    val noteId: Long,
    val title: String,
    val content: String,
    val category: String,
    val isLocked: Boolean,
    val updatedAtMillis: Long?
)

/** Actions that need the lock password before they run. */
sealed interface ProtectedAction {
    val noteId: Long

    data class Open(override val noteId: Long) : ProtectedAction
    data class RemoveLock(override val noteId: Long) : ProtectedAction
    data class Delete(override val noteId: Long) : ProtectedAction
}

data class AuthPrompt(
    val action: ProtectedAction,
    val errorMessage: String? = null,
    /** [SystemClock.elapsedRealtime] until which input is refused; 0 when not throttled. */
    val lockedOutUntil: Long = 0L,
    /** Bumped on every failed attempt so the dialog can clear its input. */
    val failedAttemptId: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = _searchQuery
        .debounce { if (it.isBlank()) 0L else 150L }
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.observeNotePreviews(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var editor by mutableStateOf<NoteEditorState?>(null)
        private set

    var authPrompt by mutableStateOf<AuthPrompt?>(null)
        private set

    /** Last deleted note, kept so the user can undo. */
    var recentlyDeleted by mutableStateOf<NoteEntity?>(null)
        private set

    // All writes go through this lock so a quick "toggle lock, then close" can't insert twice.
    private val writeMutex = Mutex()
    private val insertedIdsBySession = mutableMapOf<Long, Long>()

    private var failedAttempts = 0
    private var lockedOutUntil = 0L

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // ---- Opening / creating -------------------------------------------------------------

    fun createNote() {
        editor = NoteEditorState(
            sessionKey = SystemClock.elapsedRealtimeNanos(),
            noteId = 0L,
            title = "",
            content = "",
            category = NoteCategory.IDEAS.name,
            isLocked = false,
            updatedAtMillis = null
        )
    }

    fun onNoteClick(note: NoteEntity) {
        if (note.isLocked) {
            requestAuth(ProtectedAction.Open(note.id))
        } else {
            openNote(note.id)
        }
    }

    private fun openNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id) ?: return@launch
            editor = NoteEditorState(
                sessionKey = SystemClock.elapsedRealtimeNanos(),
                noteId = note.id,
                title = note.title,
                content = note.content,
                category = note.categoryIcon,
                isLocked = note.isLocked,
                updatedAtMillis = note.updatedAtMillis
            )
        }
    }

    // ---- Editor ----------------------------------------------------------------------

    fun onTitleChange(value: String) {
        editor = editor?.copy(title = value)
    }

    fun onContentChange(value: String) {
        editor = editor?.copy(content = value)
    }

    fun onCategoryChange(category: NoteCategory) {
        editor = editor?.copy(category = category.name)
    }

    /**
     * Toggling inside the editor needs no password: a locked note can only be open here
     * after the password was entered.
     */
    fun toggleLockInEditor() {
        val current = editor ?: return
        val updated = current.copy(isLocked = !current.isLocked)
        editor = updated
        viewModelScope.launch { persist(updated) }
    }

    fun closeEditor() {
        val current = editor ?: return
        editor = null
        viewModelScope.launch { persist(current) }
    }

    fun deleteFromEditor() {
        val current = editor ?: return
        editor = null
        viewModelScope.launch {
            val id = writeMutex.withLock { resolveId(current) }
            if (id != 0L) delete(id)
        }
    }

    /**
     * Called when the app goes to the background: saves the draft, and closes a locked
     * note so its content isn't left on screen.
     */
    fun onAppBackgrounded() {
        val current = editor ?: return
        if (current.isLocked) editor = null
        viewModelScope.launch { persist(current) }
    }

    private fun resolveId(state: NoteEditorState): Long =
        if (state.noteId != 0L) state.noteId else insertedIdsBySession[state.sessionKey] ?: 0L

    private suspend fun persist(state: NoteEditorState) = withContext(NonCancellable) {
        writeMutex.withLock {
            val title = state.title.trim()
            val content = state.content.trimEnd()
            val isBlank = title.isEmpty() && content.isBlank()
            val id = resolveId(state)

            if (id == 0L) {
                if (isBlank) return@withLock
                val newId = repository.insertNote(
                    NoteEntity(
                        title = title,
                        content = content,
                        categoryIcon = state.category,
                        isLocked = state.isLocked,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
                insertedIdsBySession[state.sessionKey] = newId
                editor?.let { if (it.sessionKey == state.sessionKey) editor = it.copy(noteId = newId) }
                return@withLock
            }

            val existing = repository.getNote(id) ?: return@withLock // deleted meanwhile
            // An existing note that was emptied keeps its last text rather than being wiped.
            val newTitle = if (isBlank) existing.title else title
            val newContent = if (isBlank) existing.content else content
            val textChanged = existing.title != newTitle ||
                existing.content != newContent ||
                existing.categoryIcon != state.category
            if (!textChanged && existing.isLocked == state.isLocked) return@withLock

            repository.updateNote(
                existing.copy(
                    title = newTitle,
                    content = newContent,
                    categoryIcon = state.category,
                    isLocked = state.isLocked,
                    updatedAtMillis = if (textChanged) System.currentTimeMillis() else existing.updatedAtMillis
                )
            )
        }
    }

    // ---- Grid card actions --------------------------------------------------------------

    fun onToggleLockClick(note: NoteEntity) {
        if (note.isLocked) {
            requestAuth(ProtectedAction.RemoveLock(note.id))
        } else {
            viewModelScope.launch { writeMutex.withLock { repository.setLocked(note.id, true) } }
        }
    }

    fun onDeleteClick(note: NoteEntity) {
        if (note.isLocked) {
            requestAuth(ProtectedAction.Delete(note.id))
        } else {
            viewModelScope.launch { delete(note.id) }
        }
    }

    private suspend fun delete(id: Long) = withContext(NonCancellable) {
        writeMutex.withLock {
            val note = repository.getNote(id) ?: return@withLock
            repository.deleteNote(note)
            recentlyDeleted = note
        }
    }

    fun undoDelete() {
        val note = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch { writeMutex.withLock { repository.insertNote(note) } }
    }

    fun clearRecentlyDeleted() {
        recentlyDeleted = null
    }

    // ---- Password ----------------------------------------------------------------------

    private fun requestAuth(action: ProtectedAction) {
        authPrompt = AuthPrompt(action = action, lockedOutUntil = activeLockout())
    }

    fun dismissAuth() {
        authPrompt = null
    }

    fun submitPassword(input: String) {
        val prompt = authPrompt ?: return
        val now = SystemClock.elapsedRealtime()
        if (now < lockedOutUntil) {
            authPrompt = prompt.copy(lockedOutUntil = lockedOutUntil)
            return
        }

        if (repository.verifyPassword(input)) {
            failedAttempts = 0
            authPrompt = null
            when (val action = prompt.action) {
                is ProtectedAction.Open -> openNote(action.noteId)
                is ProtectedAction.RemoveLock -> viewModelScope.launch {
                    writeMutex.withLock { repository.setLocked(action.noteId, false) }
                }
                is ProtectedAction.Delete -> viewModelScope.launch { delete(action.noteId) }
            }
            return
        }

        failedAttempts++
        val attemptsLeft = MAX_ATTEMPTS - (failedAttempts % MAX_ATTEMPTS)
        val message = if (attemptsLeft == MAX_ATTEMPTS) {
            // Every MAX_ATTEMPTS failures, refuse input for a doubling period of time.
            val round = failedAttempts / MAX_ATTEMPTS
            lockedOutUntil = now + BASE_LOCKOUT_MS * (1L shl (round - 1).coerceAtMost(6))
            "Too many wrong attempts."
        } else {
            "Wrong password. $attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} left."
        }
        authPrompt = prompt.copy(
            errorMessage = message,
            lockedOutUntil = activeLockout(),
            failedAttemptId = prompt.failedAttemptId + 1
        )
    }

    private fun activeLockout(): Long =
        if (SystemClock.elapsedRealtime() < lockedOutUntil) lockedOutUntil else 0L

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCKOUT_MS = 30_000L
    }
}
