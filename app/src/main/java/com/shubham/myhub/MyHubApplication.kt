package com.shubham.myhub

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.work.Configuration
import com.shubham.myhub.data.local.AppDatabase
import com.shubham.myhub.data.local.preferences.UnlockAttemptStore
import com.shubham.myhub.data.local.preferences.UserPreferences
import com.shubham.myhub.data.repository.NoteRepository
import com.shubham.myhub.data.repository.TimetableRepository
import com.shubham.myhub.data.repository.TodoRepository
import com.shubham.myhub.widget.TodoWidget

// Configuration.Provider lets WorkManager (used internally by the widget) start on demand
// instead of at every app launch.
class MyHubApplication : Application(), Configuration.Provider {

    val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferences by lazy { UserPreferences(this) }

    val todoRepository by lazy {
        TodoRepository(
            todoDao = database.todoDao(),
            goalDao = database.goalDao(),
            onTodosChanged = { TodoWidget().updateAll(this) }
        )
    }

    val noteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    val unlockAttemptStore by lazy { UnlockAttemptStore(this) }

    val timetableRepository by lazy {
        TimetableRepository(database.timetableDao())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyHubApplication
            private set
    }
}
