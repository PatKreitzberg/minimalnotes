package com.wyldsoft.notes.backend.database.dao

import androidx.room.*
import com.wyldsoft.notes.backend.database.entities.Shape
import kotlinx.coroutines.flow.Flow

@Dao
interface ShapeDao {
    @Query("SELECT * FROM shapes WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getShapesInNote(noteId: String): Flow<List<Shape>>

    @Query("SELECT * FROM shapes WHERE noteId = :noteId ORDER BY createdAt ASC")
    suspend fun getShapesInNoteSync(noteId: String): List<Shape>

    @Query("SELECT * FROM shapes WHERE id = :id")
    suspend fun getShapeById(id: String): Shape?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShape(shape: Shape)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShapes(shapes: List<Shape>)

    @Update
    suspend fun updateShape(shape: Shape)

    @Delete
    suspend fun deleteShape(shape: Shape)

    @Query("DELETE FROM shapes WHERE id = :shapeId")
    suspend fun deleteShapeById(shapeId: String)

    @Query("DELETE FROM shapes WHERE noteId = :noteId")
    suspend fun deleteAllShapesInNote(noteId: String)

    @Query("SELECT COUNT(*) FROM shapes WHERE noteId = :noteId")
    suspend fun getShapeCountInNote(noteId: String): Int
}