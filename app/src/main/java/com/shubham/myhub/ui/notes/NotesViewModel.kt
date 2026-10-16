package com.shubham.myhub.ui.notes

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.myhub.data.local.entity.NoteEntity
import com.shubham.myhub.data.local.preferences.UnlockAttemptStore
import com.shubham.myhub.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val updatedAtMillis: Long?,
    val isHidden: Boolean = false,
    val isPinned: Boolean = false
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
    private val repository: NoteRepository,
    private val unlockAttempts: UnlockAttemptStore
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** True while showing hidden notes (opened by long-pressing "+"). */
    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = combine(
        _searchQuery.debounce { if (it.isBlank()) 0L else 150L }.distinctUntilChanged(),
        _showHidden
    ) { query, hidden -> query to hidden }
        .flatMapLatest { (query, hidden) -> repository.observeNotePreviews(query, hidden) }
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

    /** Note just hidden or unhidden from the grid (it vanished from view), for the undo snackbar. */
    var recentlyHiddenChange by mutableStateOf<HiddenChange?>(null)
        private set

    data class HiddenChange(val noteId: Long, val nowHidden: Boolean)

    // All writes go through this lock so a quick "toggle lock, then close" can't insert twice.
    private val writeMutex = Mutex()
    private val insertedIdsBySession = mutableMapOf<Long, Long>()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // ---- Hidden notes ------------------------------------------------------------------

    fun toggleHiddenMode() {
        _showHidden.value = !_showHidden.value
    }

    /** Leaves the hidden view, e.g. when the user switches tab or the app goes to the background. */
    fun exitHiddenMode() {
        _showHidden.value = false
    }

    /** Hide/unhide from a grid card. No password needed: hiding never reveals content. */
    fun onToggleHiddenClick(note: NoteEntity) {
        setHidden(note.id, !note.isHidden)
    }

    private fun setHidden(id: Long, hidden: Boolean) {
        viewModelScope.launch {
            writeMutex.withLock { repository.setHidden(id, hidden) }
            recentlyHiddenChange = HiddenChange(id, hidden)
        }
    }

    /** Pin/unpin from a grid card. Doesn't change the note's "edited" time. */
    fun onTogglePinClick(note: NoteEntity) {
        viewModelScope.launch { writeMutex.withLock { repository.setPinned(note.id, !note.isPinned) } }
    }

    fun undoHiddenChange() {
        val change = recentlyHiddenChange ?: return
        recentlyHiddenChange = null
        viewModelScope.launch { writeMutex.withLock { repository.setHidden(change.noteId, !change.nowHidden) } }
    }

    fun clearRecentlyHiddenChange() {
        recentlyHiddenChange = null
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
            updatedAtMillis = null,
            // A note created while viewing hidden notes is hidden too.
            isHidden = _showHidden.value
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
                updatedAtMillis = note.updatedAtMillis,
                isHidden = note.isHidden,
                isPinned = note.isPinned
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

    fun togglePinInEditor() {
        val current = editor ?: return
        val updated = current.copy(isPinned = !current.isPinned)
        editor = updated
        viewModelScope.launch { persist(updated) }
    }

    fun toggleHiddenInEditor() {
        val current = editor ?: return
        val updated = current.copy(isHidden = !current.isHidden)
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
        exitHiddenMode()
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
                        updatedAtMillis = System.currentTimeMillis(),
                        isHidden = state.isHidden,
                        isPinned = state.isPinned
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
            if (!textChanged &&
                existing.isLocked == state.isLocked &&
                existing.isHidden == state.isHidden &&
                existing.isPinned == state.isPinned
            ) {
                return@withLock
            }

            repository.updateNote(
                existing.copy(
                    title = newTitle,
                    content = newContent,
                    categoryIcon = state.category,
                    isLocked = state.isLocked,
                    isHidden = state.isHidden,
                    isPinned = state.isPinned,
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
        val lockout = activeLockout()
        if (lockout != 0L) {
            authPrompt = prompt.copy(lockedOutUntil = lockout)
            return
        }

        if (repository.verifyPassword(input)) {
            unlockAttempts.reset()
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

        // Saved to disk before reacting, so leaving or killing the app can't undo the attempt.
        val failedAttempts = unlockAttempts.failedAttempts + 1
        val attemptsLeft = MAX_ATTEMPTS - (failedAttempts % MAX_ATTEMPTS)
        val isLockoutRound = attemptsLeft == MAX_ATTEMPTS
        // Every MAX_ATTEMPTS failures, refuse input for a doubling period of time (capped at 32 min).
        val lockoutMillis = if (isLockoutRound) {
            val round = failedAttempts / MAX_ATTEMPTS
            BASE_LOCKOUT_MS * (1L shl (round - 1).coerceAtMost(6))
        } else {
            0L
        }
        unlockAttempts.recordFailure(failedAttempts, lockoutMillis)

        val message = if (isLockoutRound) {
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

    private fun activeLockout(): Long = unlockAttempts.activeLockoutUntil()

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCKOUT_MS = 30_000L
    }
}
