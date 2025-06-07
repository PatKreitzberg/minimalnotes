package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.editor.EditorState
import com.wyldsoft.notes.utils.EraserUtils
import com.wyldsoft.notes.utils.PartialRefreshEraserManager
import com.wyldsoft.notes.render.RendererHelper
import com.onyx.android.sdk.rx.RxManager

/**
 * Manages eraser functionality for the Onyx drawing system including partial refresh optimization
 * Handles eraser mode state, erasing sessions, and optimized screen refresh during erasing
 */
class OnyxEraserManager(
    private val shapeManager: OnyxShapeManager,
    private val renderingManager: OnyxRenderingManager
) {
    companion object {
        private const val TAG = "OnyxEraserManager"
    }

    // Erasing state
    private var eraserModeEnabled = false
    private var isErasingInProgress = false
    private var currentErasingSession = EraserUtils.ErasingSession()
    private var eraserPath = mutableListOf<TouchPoint>()

    // Partial refresh manager for optimized erasing
    private val partialRefreshManager: PartialRefreshEraserManager by lazy {
        PartialRefreshEraserManager(getRxManager(), getRendererHelper())
    }

    /**
     * Set eraser mode enabled/disabled
     * @param enabled True to enable eraser mode, false to disable
     */
    fun setEraserMode(enabled: Boolean) {
        if (eraserModeEnabled != enabled) {
            eraserModeEnabled = enabled
            EditorState.setEraserMode(enabled)
            Log.d(TAG, "Eraser mode set to: $enabled")
        }
    }

    /**
     * Check if eraser mode is currently enabled
     * @return True if eraser mode is enabled
     */
    fun isEraserModeEnabled(): Boolean = eraserModeEnabled

    /**
     * Check if erasing operation is currently in progress
     * @return True if actively erasing
     */
    fun isErasingInProgress(): Boolean = isErasingInProgress

    /**
     * Begin an erasing session
     * @param touchPoint Initial touch point where erasing starts
     */
    fun beginErasing(touchPoint: TouchPoint?) {
        Log.d(TAG, "Beginning erasing session")
        isErasingInProgress = true

        // Initialize new erasing session
        currentErasingSession.clear()
        eraserPath.clear()

        // Add initial touch point to eraser path
        touchPoint?.let { point ->
            eraserPath.add(point)
        }

        EditorState.notifyErasingStarted()
    }

    /**
     * End the current erasing session
     * @param touchPoint Final touch point where erasing ends
     * @param surfaceView SurfaceView to refresh after erasing
     */
    fun endErasing(touchPoint: TouchPoint?, surfaceView: android.view.SurfaceView?) {
        Log.d(TAG, "Ending erasing session")
        isErasingInProgress = false

        // Add final touch point to eraser path
        touchPoint?.let { point ->
            eraserPath.add(point)
        }

        // Log erasing statistics
        if (currentErasingSession.hasErasedShapes()) {
            Log.d(TAG, "Erasing session completed - ${currentErasingSession.erasedShapes.size} shapes erased")
        }

        // Enable EpdController post as requested
        EpdController.enablePost(surfaceView, 1)

        // Perform optimized refresh
        performOptimizedRefresh(surfaceView)

        // Clear session data
        currentErasingSession.clear()
        eraserPath.clear()

        EditorState.notifyErasingEnded()
    }

    /**
     * Process eraser movement with individual touch point
     * @param touchPoint Touch point from eraser movement
     */
    fun processEraserMovement(touchPoint: TouchPoint?) {
        touchPoint?.let { point ->
            eraserPath.add(point)

            // Find shapes to erase at this point
            val availableShapes = shapeManager.getShapesSnapshot()
            val newShapesToErase = EraserUtils.findShapesToEraseAtPoint(point, availableShapes)

            if (newShapesToErase.isNotEmpty()) {
                eraseShapesImmediately(newShapesToErase)
                Log.d(TAG, "Erased ${newShapesToErase.size} shapes at point move")
            }
        }
    }

    /**
     * Process eraser movement with touch point list
     * @param touchPointList List of touch points from eraser movement
     */
    fun processEraserMovement(touchPointList: TouchPointList?) {
        // do nothing, will erase once user stops erasing

//        touchPointList?.let { eraserPathList ->
//            Log.d(TAG, "Processing eraser movement with ${eraserPathList.size()} points")
//
//            // Add all points to eraser path
//            eraserPath.addAll(eraserPathList.points)
//
//            // Find shapes to erase
//            val availableShapes = shapeManager.getShapesSnapshot()
//            val newShapesToErase = EraserUtils.findShapesToErase(eraserPathList, availableShapes)
//
//            if (newShapesToErase.isNotEmpty()) {
//                eraseShapesImmediately(newShapesToErase)
//                Log.d(TAG, "Erased ${newShapesToErase.size} shapes, total erased: ${currentErasingSession.erasedShapes.size}")
//            } else {
//                Log.d(TAG, "No new shapes to erase at this point")
//            }
//        }
    }

    /**
     * Get the current erasing session for database operations
     * @return Current erasing session
     */
    fun getCurrentErasingSession(): EraserUtils.ErasingSession = currentErasingSession

    /**
     * Immediately erase shapes and update the session
     * @param shapesToErase Collection of shapes to erase
     */
    private fun eraseShapesImmediately(shapesToErase: Collection<com.wyldsoft.notes.editorview.drawing.shape.DrawingShape>) {
        // Remove shapes from shape manager
        shapeManager.removeShapes(shapesToErase)

        // Add to erasing session
        shapesToErase.forEach { shape ->
            currentErasingSession.addErasedShape(shape)
        }

        // Trigger immediate partial refresh
        performOptimizedRefresh(null)
    }

    /**
     * Perform optimized partial refresh for erased areas
     * @param surfaceView Optional surface view for refresh
     */
    private fun performOptimizedRefresh(surfaceView: android.view.SurfaceView?) {
        val refreshBounds = partialRefreshManager.calculateCombinedRefreshBounds(
            currentErasingSession.erasedShapes,
            eraserPath,
            20f // Default eraser radius
        )

        if (!refreshBounds.isEmpty) {
            surfaceView?.let { sv ->
                partialRefreshManager.performPartialRefresh(
                    sv,
                    refreshBounds,
                    shapeManager.getAllShapes()
                )
                Log.d(TAG, "Partial refresh completed for bounds: $refreshBounds")
            }
        } else {
            Log.d(TAG, "No refresh bounds calculated, skipping refresh")
        }
    }

    /**
     * Toggle eraser mode programmatically
     * @return New eraser mode state
     */
    fun toggleEraserMode(): Boolean {
        setEraserMode(!eraserModeEnabled)
        return eraserModeEnabled
    }

    /**
     * Get current eraser path for debugging/analysis
     * @return Current eraser path points
     */
    fun getCurrentEraserPath(): List<TouchPoint> = eraserPath.toList()

    /**
     * Get statistics about current erasing session
     * @return Map of session statistics
     */
    fun getErasingSessionStats(): Map<String, Any> {
        return mapOf(
            "eraserModeEnabled" to eraserModeEnabled,
            "isErasingInProgress" to isErasingInProgress,
            "erasedShapesCount" to currentErasingSession.erasedShapes.size,
            "eraserPathPoints" to eraserPath.size,
            "hasErasedShapes" to currentErasingSession.hasErasedShapes()
        )
    }

    /**
     * Get RxManager instance for partial refresh operations
     */
    private fun getRxManager(): RxManager {
        return RxManager.Builder.sharedSingleThreadManager()
    }

    /**
     * Get RendererHelper instance for rendering operations
     */
    private fun getRendererHelper(): RendererHelper {
        return renderingManager.getRendererHelper()
    }

    /**
     * Force a complete refresh (fallback for when partial refresh isn't suitable)
     * @param surfaceView Surface view to refresh
     */
    fun forceCompleteRefresh(surfaceView: android.view.SurfaceView?) {
        surfaceView?.let { sv ->
            renderingManager.forceScreenRefresh(sv, shapeManager.getAllShapes())
        }
    }

    /**
     * Clear the current erasing session without saving
     */
    fun clearErasingSession() {
        currentErasingSession.clear()
        eraserPath.clear()
        isErasingInProgress = false
        Log.d(TAG, "Cleared erasing session")
    }

    /**
     * Cleanup eraser manager resources
     */
    fun cleanup() {
        clearErasingSession()
        eraserModeEnabled = false
        Log.d(TAG, "Cleaned up eraser manager resources")
    }
}