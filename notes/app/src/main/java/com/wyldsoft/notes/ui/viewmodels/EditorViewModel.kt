package com.wyldsoft.notes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wyldsoft.notes.backend.database.repository.NotesRepository
import com.wyldsoft.notes.backend.database.entities.Note
import com.wyldsoft.notes.backend.database.entities.Notebook
import com.wyldsoft.notes.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the EditorView that manages the current note
 */
class EditorViewModel(
    private val repository: NotesRepository,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _currentNotebook = MutableStateFlow<Notebook?>(null)
    val currentNotebook: StateFlow<Notebook?> = _currentNotebook.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Observe navigation changes
        _currentNote.value = navigationManager.currentNote
        _currentNotebook.value = navigationManager.currentNotebook
    }

    fun updateCurrentNote() {
        _currentNote.value = navigationManager.currentNote
        _currentNotebook.value = navigationManager.currentNotebook
    }

    fun saveNoteTitle(title: String) {
        viewModelScope.launch {
            try {
                _currentNote.value?.let { note ->
                    val updatedNote = note.copy(title = title)
                    repository.updateNote(updatedNote)
                    _currentNote.value = updatedNote
                }
            } catch (e: Exception) {
                _error.value = "Error saving note title: ${e.message}"
            }
        }
    }

    fun navigateBack() {
        navigationManager.navigateBack()
    }

    fun clearError() {
        _error.value = null
    }

    class Factory(
        private val repository: NotesRepository,
        private val navigationManager: NavigationManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditorViewModel::class.java)) {
                return EditorViewModel(repository, navigationManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}