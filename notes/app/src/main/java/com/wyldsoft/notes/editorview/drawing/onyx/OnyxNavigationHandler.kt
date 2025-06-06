package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import com.wyldsoft.notes.backend.database.entities.Note

/**
 * Handles navigation state and note management for the Onyx drawing system
 * Manages the current note context and coordinates with database operations
 */
class OnyxNavigationHandler(
    private val databaseManager: OnyxDatabaseManager
) {
    companion object {
        private const val TAG = "OnyxNavigationHandler"
    }

    // Current note being edited
    private var currentNote: Note? = null

    // Navigation state tracking
    private var isNavigating = false

    /**
     * Set the current note and load its associated data
     * @param note The note to set as current
     */
    fun setCurrentNote(note: Note) {
        if (currentNote?.id == note.id) {
            Log.d(TAG, "Note ${note.id} is already current, skipping load")
            return
        }

        Log.d(TAG, "Setting current note: ${note.id} - ${note.title}")

        val previousNote = currentNote
        currentNote = note

        // Notify database manager of note change
        // The shape manager will be passed to database manager in the calling activity
        Log.d(TAG, "Note changed from ${previousNote?.id} to ${note.id}")
    }

    /**
     * Get the currently active note
     * @return Current note or null if none set
     */
    fun getCurrentNote(): Note? = currentNote

    /**
     * Get the current note ID
     * @return Current note ID or null if no note is set
     */
    fun getCurrentNoteId(): String? = currentNote?.id

    /**
     * Get the current note title
     * @return Current note title or empty string if no note is set
     */
    fun getCurrentNoteTitle(): String = currentNote?.title ?: ""

    /**
     * Check if a note is currently loaded
     * @return True if a note is currently set
     */
    fun hasCurrentNote(): Boolean = currentNote != null

    /**
     * Check if currently navigating between notes
     * @return True if navigation is in progress
     */
    fun isNavigating(): Boolean = isNavigating

    /**
     * Set navigation state
     * @param navigating True if navigation is starting, false if complete
     */
    fun setNavigating(navigating: Boolean) {
        isNavigating = navigating
        Log.d(TAG, "Navigation state set to: $navigating")
    }

    /**
     * Prepare for navigation away from current note
     * This should be called before switching to a different note or view
     */
    fun prepareForNavigation() {
        Log.d(TAG, "Preparing for navigation away from current note")
        setNavigating(true)

        // Save current state before navigating away
        currentNote?.let { note ->
            Log.d(TAG, "Saving state for note ${note.id} before navigation")
            // The actual saving will be coordinated through the activity
        }
    }

    /**
     * Complete navigation process
     * This should be called after navigation is complete
     */
    fun completeNavigation() {
        Log.d(TAG, "Navigation completed")
        setNavigating(false)
    }

    /**
     * Handle note title update
     * @param newTitle New title for the current note
     */
    fun updateNoteTitle(newTitle: String) {
        currentNote?.let { note ->
            if (note.title != newTitle) {
                val updatedNote = note.copy(title = newTitle)
                currentNote = updatedNote
                Log.d(TAG, "Updated note title to: $newTitle")

                // The actual database update should be handled by a higher-level component
                // to avoid circular dependencies
            }
        }
    }

    /**
     * Clear the current note (used when returning to home view)
     */
    fun clearCurrentNote() {
        val previousNoteId = currentNote?.id
        currentNote = null
        Log.d(TAG, "Cleared current note (was: $previousNoteId)")
    }

    /**
     * Get note information for logging/debugging
     * @return String representation of current note state
     */
    fun getNoteInfo(): String {
        return currentNote?.let { note ->
            "Note[id=${note.id}, title='${note.title}', created=${note.createdAt}, updated=${note.updatedAt}]"
        } ?: "No current note"
    }

    /**
     * Validate that the current note is in a consistent state
     * @return True if current note state is valid
     */
    fun validateCurrentNote(): Boolean {
        val note = currentNote
        if (note == null) {
            Log.w(TAG, "No current note set")
            return false
        }

        if (note.id.isBlank()) {
            Log.e(TAG, "Current note has blank ID")
            return false
        }

        if (note.title.isBlank()) {
            Log.w(TAG, "Current note has blank title")
        }

        return true
    }
}