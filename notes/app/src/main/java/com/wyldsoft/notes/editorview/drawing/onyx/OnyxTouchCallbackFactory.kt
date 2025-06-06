package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.editor.EditorState

/**
 * Factory class that creates touch callbacks for the Onyx drawing system
 * Coordinates between different managers based on drawing vs erasing mode
 * This class serves as the bridge between touch input and the appropriate manager
 */
object OnyxTouchCallbackFactory {
    private const val TAG = "OnyxTouchCallbackFactory"

    /**
     * Create a touch callback that coordinates between all managers
     * @param shapeManager Manager for shape operations
     * @param eraserManager Manager for eraser operations
     * @param databaseManager Manager for database operations
     * @param renderingManager Manager for rendering operations
     * @param navigationHandler Handler for navigation state
     * @return Configured touch callback
     */
    fun create(
        shapeManager: OnyxShapeManager,
        eraserManager: OnyxEraserManager,
        databaseManager: OnyxDatabaseManager,
        renderingManager: OnyxRenderingManager,
        navigationHandler: OnyxNavigationHandler
    ): com.onyx.android.sdk.pen.RawInputCallback {

        return object : com.onyx.android.sdk.pen.RawInputCallback() {

            override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleBeginErasing(touchPoint, eraserManager)
                } else {
                    handleBeginDrawing(touchPoint)
                }
            }

            override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleEndErasing(touchPoint, eraserManager, databaseManager, navigationHandler, renderingManager)
                } else {
                    handleEndDrawing(touchPoint, shapeManager, databaseManager, navigationHandler, renderingManager)
                }
            }

            override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleEraserMovement(touchPoint, eraserManager)
                } else {
                    // Handle drawing move events if needed
                }
            }

            override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleEraserPathReceived(touchPointList, eraserManager)
                } else {
                    handleDrawingPathReceived(touchPointList, shapeManager, renderingManager)
                }
            }

            override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint?) {
                Log.d(TAG, "onBeginRawErasing called")
                handleBeginErasing(touchPoint, eraserManager)
            }

            override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint?) {
                Log.d(TAG, "onEndRawErasing called")
                handleEndErasing(touchPoint, eraserManager, databaseManager, navigationHandler, renderingManager)
            }

            override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {
                handleEraserMovement(touchPoint, eraserManager)
            }

            override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {
                Log.d(TAG, "onRawErasingTouchPointListReceived called with ${touchPointList?.size() ?: 0} points")
                handleEraserPathReceived(touchPointList, eraserManager)
            }
        }
    }

    /**
     * Handle beginning of drawing operation
     */
    private fun handleBeginDrawing(touchPoint: TouchPoint?) {
        Log.d(TAG, "Beginning drawing operation")
        EditorState.notifyDrawingStarted()
    }

    /**
     * Handle end of drawing operation
     */
    private fun handleEndDrawing(
        touchPoint: TouchPoint?,
        shapeManager: OnyxShapeManager,
        databaseManager: OnyxDatabaseManager,
        navigationHandler: OnyxNavigationHandler,
        renderingManager: OnyxRenderingManager
    ) {
        Log.d(TAG, "Ending drawing operation")

        // Save the new shape to database if we have a current note and shapes exist
        if (navigationHandler.hasCurrentNote() &&
            shapeManager.hasShapes() &&
            !databaseManager.isCurrentlyLoading()) {

            // The newest shape is the last one added
            val allShapes = shapeManager.getAllShapes()
            if (allShapes.isNotEmpty()) {
                // For now, we'll save all shapes - in a more sophisticated implementation,
                // we could track which shape is the new one
                databaseManager.saveCurrentState()
            }
        }

        // Force screen refresh and notify drawing ended
        EditorState.notifyDrawingEnded()
    }

    /**
     * Handle drawing path received (list of touch points)
     */
    private fun handleDrawingPathReceived(
        touchPointList: TouchPointList?,
        shapeManager: OnyxShapeManager,
        renderingManager: OnyxRenderingManager
    ) {
        touchPointList?.points?.let { points ->
            Log.d(TAG, "Drawing path received with ${points.size} points")

            // This would need access to current pen profile - should be passed from activity
            // For now, we'll let the activity handle the actual shape creation
            // and just log the event
        }
    }

    /**
     * Handle beginning of erasing operation
     */
    private fun handleBeginErasing(touchPoint: TouchPoint?, eraserManager: OnyxEraserManager) {
        Log.d(TAG, "Beginning erasing operation")
        eraserManager.beginErasing(touchPoint)
    }

    /**
     * Handle end of erasing operation
     */
    private fun handleEndErasing(
        touchPoint: TouchPoint?,
        eraserManager: OnyxEraserManager,
        databaseManager: OnyxDatabaseManager,
        navigationHandler: OnyxNavigationHandler,
        renderingManager: OnyxRenderingManager
    ) {
        Log.d(TAG, "Ending erasing operation")

        // Get surface view from rendering manager if needed
        val surfaceView: android.view.SurfaceView? = null // Would need to be passed in
        eraserManager.endErasing(touchPoint, surfaceView)

        // Update database if we erased shapes and have a current note
        val erasingSession = eraserManager.getCurrentErasingSession()
        if (erasingSession.hasErasedShapes() && navigationHandler.hasCurrentNote()) {
            navigationHandler.getCurrentNote()?.let { note ->
                // The eraser manager should have already updated the shape manager,
                // so we just need to save the current state
                databaseManager.saveCurrentState()
            }
        }
    }

    /**
     * Handle eraser movement (single point)
     */
    private fun handleEraserMovement(touchPoint: TouchPoint?, eraserManager: OnyxEraserManager) {
        eraserManager.processEraserMovement(touchPoint)
    }

    /**
     * Handle eraser path received (list of points)
     */
    private fun handleEraserPathReceived(touchPointList: TouchPointList?, eraserManager: OnyxEraserManager) {
        eraserManager.processEraserMovement(touchPointList)
    }
}