package com.shubham.myhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shubham.myhub.ui.navigation.AppNavigation
import com.shubham.myhub.ui.notes.NotesViewModel
import com.shubham.myhub.ui.theme.MyHubTheme
import com.shubham.myhub.ui.timetable.TimetableViewModel
import com.shubham.myhub.ui.todo.TodoViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as MyHubApplication }

    private val todoViewModel: TodoViewModel by viewModels {
        ViewModelFactory { TodoViewModel(app.todoRepository) }
    }

    private val notesViewModel: NotesViewModel by viewModels {
        ViewModelFactory { NotesViewModel(app.noteRepository, app.unlockAttemptStore) }
    }

    private val timetableViewModel: TimetableViewModel by viewModels {
        ViewModelFactory { TimetableViewModel(app.timetableRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyHubTheme {
                AppNavigation(
                    todoViewModel = todoViewModel,
                    notesViewModel = notesViewModel,
                    timetableViewModel = timetableViewModel
                )
            }
        }
    }
}

class ViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator() as T
    }
}
