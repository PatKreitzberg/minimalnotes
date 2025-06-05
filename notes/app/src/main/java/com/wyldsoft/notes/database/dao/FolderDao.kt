package com.wyldsoft.notes.database.dao

import androidx.room.*
import com.wyldsoft.notes.database.entities.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY name ASC")
    fun getSubfolders(parentId: String): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): Folder?

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY name ASC")
    suspend fun getSubfoldersSync(parentId: String): List<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    // Get folder hierarchy path (breadcrumb)
    @Query("""
        WITH RECURSIVE folder_path(id, name, parentFolderId, createdAt, updatedAt, level) AS (
            SELECT id, name, parentFolderId, createdAt, updatedAt, 0 as level
            FROM folders 
            WHERE id = :folderId
            
            UNION ALL
            
            SELECT f.id, f.name, f.parentFolderId, f.createdAt, f.updatedAt, fp.level + 1
            FROM folders f
            INNER JOIN folder_path fp ON f.id = fp.parentFolderId
        )
        SELECT id, name, parentFolderId, createdAt, updatedAt FROM folder_path ORDER BY level DESC
    """)
    suspend fun getFolderPath(folderId: String): List<Folder>
}
