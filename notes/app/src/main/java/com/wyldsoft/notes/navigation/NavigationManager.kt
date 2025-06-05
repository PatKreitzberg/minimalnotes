package com.wyldsoft.notes.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wyldsoft.notes.database.entities.Folder
import com.wyldsoft.notes.database.entities.Notebook
import com.wyldsoft.notes.database.entities.Note

/**
 * Manages navigation state for the app
 */
class NavigationManager {
    var currentView by mutableStateOf<AppView>(AppView.Home)
        private set

    var currentFolder by mutableStateOf<Folder?>(null)
        private set

    var currentNotebook by mutableStateOf<Notebook?>(null)
        private set

    var currentNote by mutableStateOf<Note?>(null)
        private set

    fun navigateToFolder(folder: Folder?) {
        currentFolder = folder
        currentView = AppView.Home
    }

    fun navigateToEditor(notebook: Notebook, note: Note) {
        currentNotebook = notebook
        currentNote = note
        currentView = AppView.Editor
    }

    fun navigateBack() {
        when (currentView) {
            AppView.Editor -> {
                currentNotebook = null
                currentNote = null
                currentView = AppView.Home
            }
            AppView.Home -> {
                // Navigate to parent folder if not at root
                currentFolder?.parentFolderId?.let { parentId ->
                    // This will be handled by the ViewModel
                }
            }
        }
    }

    fun getCurrentFolderId(): String {
        return currentFolder?.id ?: "root"
    }

    companion object {
        @Volatile
        private var INSTANCE: NavigationManager? = null

        fun getInstance(): NavigationManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NavigationManager()
                INSTANCE = instance
                instance
            }
        }
    }
}

sealed class AppView {
    object Home : AppView()
    object Editor : AppView()
}
