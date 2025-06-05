package com.wyldsoft.notes.database.dao

import androidx.room.*
import com.wyldsoft.notes.database.entities.Note
import com.wyldsoft.notes.database.entities.NotebookNoteReference
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN notebook_note_references nnr ON n.id = nnr.noteId
        WHERE nnr.notebookId = :notebookId
        ORDER BY nnr.addedAt ASC
    """)
    fun getNotesInNotebook(notebookId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    // Notebook-Note reference management
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addNoteToNotebook(reference: NotebookNoteReference)

    @Query("DELETE FROM notebook_note_references WHERE notebookId = :notebookId AND noteId = :noteId")
    suspend fun removeNoteFromNotebook(notebookId: String, noteId: String)

    @Query("SELECT COUNT(*) FROM notebook_note_references WHERE notebookId = :notebookId AND noteId = :noteId")
    suspend fun isNoteInNotebook(notebookId: String, noteId: String): Int

    // Get first note in notebook (for opening)
    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN notebook_note_references nnr ON n.id = nnr.noteId
        WHERE nnr.notebookId = :notebookId
        ORDER BY nnr.addedAt ASC
        LIMIT 1
    """)
    suspend fun getFirstNoteInNotebook(notebookId: String): Note?

    @Transaction
    suspend fun createNoteInNotebook(note: Note, notebookId: String) {
        insertNote(note)
        addNoteToNotebook(NotebookNoteReference(notebookId, note.id))
    }
}