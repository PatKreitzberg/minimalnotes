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
     * @param activity The main activity for accessing pen profile and surface view
     * @return Configured touch callback
     */
    fun create(
        shapeManager: OnyxShapeManager,
        eraserManager: OnyxEraserManager,
        databaseManager: OnyxDatabaseManager,
        renderingManager: OnyxRenderingManager,
        navigationHandler: OnyxNavigationHandler,
        activity: OnyxDrawingActivity
    ): com.onyx.android.sdk.pen.RawInputCallback {

        return object : com.onyx.android.sdk.pen.RawInputCallback() {

            override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleBeginErasing(touchPoint, eraserManager, activity)
                } else {
                    handleBeginDrawing(touchPoint, activity)
                }
            }

            override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
                if (eraserManager.isEraserModeEnabled()) {
                    handleEndErasing(touchPoint, eraserManager, databaseManager, navigationHandler, renderingManager, activity, shapeManager)
                } else {
                    handleEndDrawing(touchPoint, shapeManager, databaseManager, navigationHandler, renderingManager, activity)
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
                    handleDrawingPathReceived(touchPointList, shapeManager, renderingManager, activity)
                }
            }

            override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint?) {
                Log.d(TAG, "onBeginRawErasing called")
                handleBeginErasing(touchPoint, eraserManager, activity)
            }

            override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint?) {
                Log.d(TAG, "onEndRawErasing called")
                handleEndErasing(touchPoint, eraserManager, databaseManager, navigationHandler, renderingManager, activity, shapeManager)
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
    private fun handleBeginDrawing(touchPoint: TouchPoint?, activity: OnyxDrawingActivity) {
        Log.d(TAG, "Beginning drawing operation")
        activity.disableFingerTouch()
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
        renderingManager: OnyxRenderingManager,
        activity: OnyxDrawingActivity
    ) {
        Log.d(TAG, "Ending drawing operation")

        activity.enableFingerTouch()

        // Save the new shape to database if we have a current note and shapes exist
        if (navigationHandler.hasCurrentNote() &&
            shapeManager.hasShapes() &&
            !databaseManager.isCurrentlyLoading()) {

            val allShapes = shapeManager.getAllShapes()
            if (allShapes.isNotEmpty()) {
                // Save the most recently added shape
                val latestShape = allShapes.last()
                databaseManager.saveShapeImmediately(latestShape, activity.currentPenProfile)
            }
        }

        // Force screen refresh
        activity.forceScreenRefresh()
        EditorState.notifyDrawingEnded()
    }

    /**
     * Handle drawing path received (list of touch points)
     */
    private fun handleDrawingPathReceived(
        touchPointList: TouchPointList?,
        shapeManager: OnyxShapeManager,
        renderingManager: OnyxRenderingManager,
        activity: OnyxDrawingActivity
    ) {
        touchPointList?.points?.let { points ->
            Log.d(TAG, "Drawing path received with ${points.size} points")

            // Create shape from touch points using current pen profile
            val shape = shapeManager.createShapeFromTouchPoints(touchPointList, activity.currentPenProfile)

            // Add shape to manager
            shapeManager.addShape(shape)

            // Render shape to bitmap
            renderingManager.renderShapeToBitmap(shape)

            // Render to screen
            activity.surfaceView?.let { sv ->
                renderingManager.renderToScreen(sv, renderingManager.getCurrentBitmap())
            }
        }
    }

    /**
     * Handle beginning of erasing operation
     */
    private fun handleBeginErasing(touchPoint: TouchPoint?, eraserManager: OnyxEraserManager, activity: OnyxDrawingActivity) {
        Log.d(TAG, "Beginning erasing operation")
        activity.disableFingerTouch()
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
        renderingManager: OnyxRenderingManager,
        activity: OnyxDrawingActivity,
        shapeManager: OnyxShapeManager
    ) {
        Log.d(TAG, "Ending erasing operation")

        activity.enableFingerTouch()

        eraserManager.endErasing(touchPoint, activity.surfaceView)

        // Update database if we erased shapes and have a current note
        val erasingSession = eraserManager.getCurrentErasingSession()
        if (erasingSession.hasErasedShapes() && navigationHandler.hasCurrentNote()) {
            // Save all remaining shapes to database
            databaseManager.saveAllShapesToDatabase(
                shapeManager.getAllShapes(),
                activity.currentPenProfile
            )
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