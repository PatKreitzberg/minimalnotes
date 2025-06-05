package com.wyldsoft.notes.database.dao

import androidx.room.*
import com.wyldsoft.notes.database.entities.Notebook
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotebooksInFolder(folderId: String): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: String): Notebook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook)

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    @Query("DELETE FROM notebooks WHERE id = :notebookId")
    suspend fun deleteNotebookById(notebookId: String)

    @Query("SELECT COUNT(*) FROM notebooks WHERE folderId = :folderId")
    suspend fun getNotebookCountInFolder(folderId: String): Int
}