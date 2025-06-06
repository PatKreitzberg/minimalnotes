package com.wyldsoft.notes.backend.database.repository

import com.wyldsoft.notes.backend.database.NotesDatabase
import com.wyldsoft.notes.backend.database.entities.*
import kotlinx.coroutines.flow.Flow
import com.aventrix.jnanoid.jnanoid.NanoIdUtils

class NotesRepository(private val database: NotesDatabase) {

    // Folder operations
    fun getRootFolders(): Flow<List<Folder>> = database.folderDao().getRootFolders()

    fun getSubfolders(parentId: String): Flow<List<Folder>> =
        database.folderDao().getSubfolders(parentId)

    suspend fun getFolderById(id: String): Folder? = database.folderDao().getFolderById(id)

    suspend fun getFolderPath(folderId: String): List<Folder> =
        database.folderDao().getFolderPath(folderId)

    suspend fun createFolder(name: String, parentFolderId: String? = null): Folder {
        val folder = Folder(
            id = NanoIdUtils.randomNanoId(),
            name = name,
            parentFolderId = parentFolderId
        )
        database.folderDao().insertFolder(folder)
        return folder
    }

    suspend fun updateFolder(folder: Folder) {
        val updatedFolder = folder.copy(updatedAt = System.currentTimeMillis())
        database.folderDao().updateFolder(updatedFolder)
    }

    suspend fun deleteFolder(folderId: String) {
        database.folderDao().deleteFolderById(folderId)
    }

    // Notebook operations
    fun getNotebooksInFolder(folderId: String): Flow<List<Notebook>> =
        database.notebookDao().getNotebooksInFolder(folderId)

    suspend fun getNotebookById(id: String): Notebook? = database.notebookDao().getNotebookById(id)

    suspend fun createNotebook(name: String, folderId: String): Notebook {
        val notebookId = NanoIdUtils.randomNanoId()
        val notebook = Notebook(
            id = notebookId,
            name = name,
            folderId = folderId
        )

        // Create the notebook
        database.notebookDao().insertNotebook(notebook)

        // Create a default empty note for the notebook
        val defaultNote = Note(
            id = NanoIdUtils.randomNanoId(),
            title = "Note 1"
        )
        database.noteDao().createNoteInNotebook(defaultNote, notebookId)

        return notebook
    }

    suspend fun updateNotebook(notebook: Notebook) {
        val updatedNotebook = notebook.copy(updatedAt = System.currentTimeMillis())
        database.notebookDao().updateNotebook(updatedNotebook)
    }

    suspend fun deleteNotebook(notebookId: String) {
        database.notebookDao().deleteNotebookById(notebookId)
    }

    // Note operations
    fun getNotesInNotebook(notebookId: String): Flow<List<Note>> =
        database.noteDao().getNotesInNotebook(notebookId)

    suspend fun getNoteById(id: String): Note? = database.noteDao().getNoteById(id)

    suspend fun getFirstNoteInNotebook(notebookId: String): Note? =
        database.noteDao().getFirstNoteInNotebook(notebookId)

    suspend fun createNote(title: String = "Untitled Note"): Note {
        val note = Note(
            id = NanoIdUtils.randomNanoId(),
            title = title
        )
        database.noteDao().insertNote(note)
        return note
    }

    suspend fun createNoteInNotebook(title: String, notebookId: String): Note {
        val note = Note(
            id = NanoIdUtils.randomNanoId(),
            title = title
        )
        database.noteDao().createNoteInNotebook(note, notebookId)
        return note
    }

    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        database.noteDao().updateNote(updatedNote)
    }

    suspend fun deleteNote(noteId: String) {
        database.noteDao().deleteNoteById(noteId)
    }

    suspend fun addNoteToNotebook(noteId: String, notebookId: String) {
        val reference = NotebookNoteReference(notebookId, noteId)
        database.noteDao().addNoteToNotebook(reference)
    }

    suspend fun removeNoteFromNotebook(noteId: String, notebookId: String) {
        database.noteDao().removeNoteFromNotebook(notebookId, noteId)
    }

    // Shape operations
    fun getShapesInNote(noteId: String): Flow<List<Shape>> =
        database.shapeDao().getShapesInNote(noteId)

    suspend fun getShapesInNoteSync(noteId: String): List<Shape> =
        database.shapeDao().getShapesInNoteSync(noteId)

    suspend fun saveShape(shape: Shape) {
        database.shapeDao().insertShape(shape)
    }

    suspend fun saveShapes(shapes: List<Shape>) {
        database.shapeDao().insertShapes(shapes)
    }

    suspend fun deleteShape(shapeId: String) {
        database.shapeDao().deleteShapeById(shapeId)
    }

    suspend fun deleteAllShapesInNote(noteId: String) {
        database.shapeDao().deleteAllShapesInNote(noteId)
    }

    companion object {
        @Volatile
        private var INSTANCE: NotesRepository? = null

        fun getInstance(database: NotesDatabase): NotesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotesRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}