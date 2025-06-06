package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.wyldsoft.notes.backend.database.DatabaseManager
import com.wyldsoft.notes.backend.database.ShapeUtils
import com.wyldsoft.notes.backend.database.entities.Note
import com.wyldsoft.notes.editorview.drawing.shape.Shape
import com.wyldsoft.notes.pen.PenProfile
import kotlinx.coroutines.launch

/**
 * Manages database operations for shape persistence in the Onyx drawing system
 * Handles loading shapes from database and saving shapes to database
 * Coordinates with the shape manager for data synchronization
 */
class OnyxDatabaseManager(
    private val activity: OnyxDrawingActivity
) {
    companion object {
        private const val TAG = "OnyxDatabaseManager"
    }

    // Database manager instance
    private val databaseManager = DatabaseManager.getInstance(activity)

    // Track loading state to avoid saving while loading
    private var isLoadingFromDatabase = false

    // Current note being edited
    private var currentNote: Note? = null

    /**
     * Set the current note and load its shapes
     * @param note The note to load shapes for
     * @param shapeManager Shape manager to populate with loaded shapes
     */
    fun setCurrentNote(note: Note, shapeManager: OnyxShapeManager) {
        currentNote = note
        loadShapesFromDatabase(note.id, shapeManager)
    }

    /**
     * Get the currently active note
     * @return Current note or null if none set
     */
    fun getCurrentNote(): Note? = currentNote

    /**
     * Load shapes from database for the specified note
     * @param noteId ID of the note to load shapes for
     * @param shapeManager Shape manager to populate with loaded shapes
     */
    fun loadShapesFromDatabase(noteId: String, shapeManager: OnyxShapeManager) {
        activity.lifecycleScope.launch {
            try {
                isLoadingFromDatabase = true
                Log.d(TAG, "Loading shapes from database for note: $noteId")

                val databaseShapes = databaseManager.repository.getShapesInNoteSync(noteId)

                // Convert database shapes to drawing shapes
                val drawingShapes = databaseShapes.map { dbShape ->
                    ShapeUtils.convertToDrawing(dbShape)
                }

                // Replace all shapes in the manager
                shapeManager.replaceAllShapes(drawingShapes)

                // Recreate the drawing from loaded shapes
                shapeManager.recreateDrawingFromShapes()

                Log.d(TAG, "Successfully loaded ${drawingShapes.size} shapes from database")
                isLoadingFromDatabase = false

            } catch (e: Exception) {
                Log.e(TAG, "Error loading shapes from database for note $noteId", e)
                isLoadingFromDatabase = false
            }
        }
    }

    /**
     * Save a single shape to the database
     * @param shape The shape to save
     * @param penProfile The pen profile used to create the shape
     */
    fun saveShapeToDatabase(shape: Shape, penProfile: PenProfile) {
        if (isLoadingFromDatabase) {
            Log.d(TAG, "Skipping save while loading from database")
            return
        }

        currentNote?.let { note ->
            activity.lifecycleScope.launch {
                try {
                    val databaseShape = ShapeUtils.convertToDatabase(shape, note.id, penProfile)
                    databaseManager.repository.saveShape(databaseShape)
                    Log.d(TAG, "Saved shape to database for note ${note.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving shape to database", e)
                }
            }
        }
    }

    /**
     * Save all current shapes to the database (full state save)
     * @param allShapes List of all current shapes
     * @param penProfile Current pen profile
     */
    fun saveAllShapesToDatabase(allShapes: List<Shape>, penProfile: PenProfile) {
        if (isLoadingFromDatabase) {
            Log.d(TAG, "Skipping save while loading from database")
            return
        }

        currentNote?.let { note ->
            activity.lifecycleScope.launch {
                try {
                    // Clear existing shapes for this note
                    databaseManager.repository.deleteAllShapesInNote(note.id)

                    // Convert and save all current shapes
                    val databaseShapes = allShapes.map { drawingShape ->
                        ShapeUtils.convertToDatabase(drawingShape, note.id, penProfile)
                    }

                    databaseManager.repository.saveShapes(databaseShapes)

                    Log.d(TAG, "Saved ${databaseShapes.size} shapes to database for note ${note.id}")

                } catch (e: Exception) {
                    Log.e(TAG, "Error saving all shapes to database", e)
                }
            }
        }
    }

    /**
     * Update database after erasing shapes
     * @param remainingShapes The shapes that remain after erasing
     * @param penProfile Current pen profile
     */
    fun updateDatabaseAfterErasing(remainingShapes: List<Shape>, penProfile: PenProfile) {
        if (isLoadingFromDatabase) {
            Log.d(TAG, "Skipping database update while loading from database")
            return
        }

        currentNote?.let { note ->
            activity.lifecycleScope.launch {
                try {
                    // Clear existing shapes for this note
                    databaseManager.repository.deleteAllShapesInNote(note.id)

                    // Save remaining shapes
                    val databaseShapes = remainingShapes.map { drawingShape ->
                        ShapeUtils.convertToDatabase(drawingShape, note.id, penProfile)
                    }

                    databaseManager.repository.saveShapes(databaseShapes)

                    Log.d(TAG, "Updated database after erasing - ${databaseShapes.size} shapes remaining")

                } catch (e: Exception) {
                    Log.e(TAG, "Error updating database after erasing", e)
                }
            }
        }
    }

    /**
     * Clear all shapes for the current note from database
     * @param noteId ID of the note to clear shapes for
     */
    fun clearAllShapesForNote(noteId: String) {
        activity.lifecycleScope.launch {
            try {
                databaseManager.repository.deleteAllShapesInNote(noteId)
                Log.d(TAG, "Cleared all shapes for note $noteId from database")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing shapes for note $noteId", e)
            }
        }
    }

    /**
     * Save the current drawing state to database
     * This is called during pause/cleanup operations
     */
    fun saveCurrentState() {
        Log.d(TAG, "saveCurrentState called - use saveAllShapesToDatabase instead")
    }

    /**
     * Save a single shape immediately after it's drawn
     * @param shape The shape that was just drawn
     * @param penProfile The pen profile used to create the shape
     */
    fun saveShapeImmediately(shape: Shape, penProfile: PenProfile) {
        saveShapeToDatabase(shape, penProfile)
    }

    /**
     * Check if currently loading from database
     * @return True if loading operation is in progress
     */
    fun isCurrentlyLoading(): Boolean = isLoadingFromDatabase
}