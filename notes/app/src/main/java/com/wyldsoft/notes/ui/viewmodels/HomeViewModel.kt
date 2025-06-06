package com.wyldsoft.notes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wyldsoft.notes.backend.database.repository.NotesRepository
import com.wyldsoft.notes.backend.database.entities.Folder
import com.wyldsoft.notes.backend.database.entities.Notebook
import com.wyldsoft.notes.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the HomeView that manages folders and notebooks
 */
class HomeViewModel(
    private val repository: NotesRepository,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _notebooks = MutableStateFlow<List<Notebook>>(emptyList())
    val notebooks: StateFlow<List<Notebook>> = _notebooks.asStateFlow()

    private val _breadcrumb = MutableStateFlow<List<Folder>>(emptyList())
    val breadcrumb: StateFlow<List<Folder>> = _breadcrumb.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentFolderContent()
    }

    fun loadCurrentFolderContent() {
        val currentFolderId = navigationManager.getCurrentFolderId()
        loadFolderContent(currentFolderId)
    }

    private fun loadFolderContent(folderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load breadcrumb first
                loadBreadcrumb(folderId)

                // Track loading state for both folders and notebooks
                var foldersLoaded = false
                var notebooksLoaded = false

                fun checkLoadingComplete() {
                    if (foldersLoaded && notebooksLoaded) {
                        _isLoading.value = false
                    }
                }

                // Load folders
                val foldersJob = launch {
                    if (folderId == "root") {
                        repository.getRootFolders().collect { folderList ->
                            _folders.value = folderList
                            if (!foldersLoaded) {
                                foldersLoaded = true
                                checkLoadingComplete()
                            }
                        }
                    } else {
                        repository.getSubfolders(folderId).collect { folderList ->
                            _folders.value = folderList
                            if (!foldersLoaded) {
                                foldersLoaded = true
                                checkLoadingComplete()
                            }
                        }
                    }
                }

                // Load notebooks
                val notebooksJob = launch {
                    repository.getNotebooksInFolder(folderId).collect { notebookList ->
                        _notebooks.value = notebookList
                        if (!notebooksLoaded) {
                            notebooksLoaded = true
                            checkLoadingComplete()
                        }
                    }
                }

            } catch (e: Exception) {
                _error.value = "Error loading folder content: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun loadBreadcrumb(folderId: String) {
        viewModelScope.launch {
            try {
                if (folderId == "root") {
                    _breadcrumb.value = emptyList()
                } else {
                    val path = repository.getFolderPath(folderId)
                    _breadcrumb.value = path
                }
            } catch (e: Exception) {
                // Handle breadcrumb loading error silently
            }
        }
    }

    fun navigateToFolder(folder: Folder) {
        navigationManager.navigateToFolder(folder)
        loadFolderContent(folder.id)
    }

    fun navigateToParentFolder() {
        val currentFolder = navigationManager.currentFolder
        if (currentFolder?.parentFolderId != null) {
            viewModelScope.launch {
                val parentFolder = repository.getFolderById(currentFolder.parentFolderId)
                navigationManager.navigateToFolder(parentFolder)
                loadFolderContent(parentFolder?.id ?: "root")
            }
        } else {
            // Navigate to root
            navigationManager.navigateToFolder(null)
            loadFolderContent("root")
        }
    }

    fun navigateToNotebook(notebook: Notebook) {
        viewModelScope.launch {
            try {
                val firstNote = repository.getFirstNoteInNotebook(notebook.id)
                if (firstNote != null) {
                    navigationManager.navigateToEditor(notebook, firstNote)
                } else {
                    _error.value = "No notes found in notebook"
                }
            } catch (e: Exception) {
                _error.value = "Error opening notebook: ${e.message}"
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                val currentFolderId = navigationManager.getCurrentFolderId()
                val parentId = if (currentFolderId == "root") null else currentFolderId

                repository.createFolder(name, parentId)
                loadCurrentFolderContent() // Refresh the view

            } catch (e: Exception) {
                _error.value = "Error creating folder: ${e.message}"
            }
        }
    }

    fun createNotebook(name: String) {
        viewModelScope.launch {
            try {
                val currentFolderId = navigationManager.getCurrentFolderId()
                repository.createNotebook(name, currentFolderId)
                loadCurrentFolderContent() // Refresh the view

            } catch (e: Exception) {
                _error.value = "Error creating notebook: ${e.message}"
            }
        }
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
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository, navigationManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

