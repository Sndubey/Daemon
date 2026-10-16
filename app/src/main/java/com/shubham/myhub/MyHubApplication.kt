package com.shubham.myhub

import android.app.Application
import com.shubham.myhub.data.local.AppDatabase
import com.shubham.myhub.data.local.preferences.UserPreferences
import com.shubham.myhub.data.repository.NoteRepository
import com.shubham.myhub.data.repository.TodoRepository

class MyHubApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferences by lazy { UserPreferences(this) }

    val todoRepository by lazy {
        TodoRepository(database.todoDao(), database.goalDao())
    }

    val noteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyHubApplication
            private set
    }
}
