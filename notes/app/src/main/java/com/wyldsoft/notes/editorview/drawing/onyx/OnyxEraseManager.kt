package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import android.graphics.PointF
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
    private val renderingManager: OnyxRenderingManager,
    private val databaseManager: OnyxDatabaseManager,
    private val getPenProfile: () -> com.wyldsoft.notes.pen.PenProfile
) {
    companion object {
        private const val TAG = "OnyxEraserManager"
    }

    // Erasing state
    private var eraserModeEnabled = false
    private var isErasingInProgress = false
    private var currentErasingSession = EraserUtils.ErasingSession()
    private var eraserPath = mutableListOf<TouchPoint>()
    
    // Viewport controller for coordinate transforms
    private var viewportController: com.wyldsoft.notes.editorview.viewport.ViewportController? = null

    // Partial refresh manager for optimized erasing
    private lateinit var partialRefreshManager: PartialRefreshEraserManager

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
     * Set the viewport controller for coordinate transformations
     * @param controller ViewportController instance
     */
    fun setViewportController(controller: com.wyldsoft.notes.editorview.viewport.ViewportController?) {
        viewportController = controller
        
        // Initialize partial refresh manager with viewport controller
        partialRefreshManager = PartialRefreshEraserManager(
            getRxManager(), 
            getRendererHelper(), 
            viewportController
        )
    }

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
     * End the current erasing session and finalize all changes
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

        // Update database with remaining shapes if any shapes were erased
        if (currentErasingSession.hasErasedShapes()) {
            Log.d(TAG, "Erasing session completed - ${currentErasingSession.erasedShapes.size} shapes erased")
            Log.d(TAG, "Total shapes remaining in shape manager: ${shapeManager.getAllShapes().size}")
            
            // Update database with remaining shapes
            updateDatabaseWithRemainingShapes()
        } else {
            Log.d(TAG, "Erasing session ended with no shapes erased")
        }

        // Enable EpdController post as requested
        EpdController.enablePost(surfaceView, 1)

        // Perform optimized refresh only if shapes were erased
        if (currentErasingSession.hasErasedShapes()) {
            performOptimizedRefresh(surfaceView)
        }

        // Clear session data
        currentErasingSession.clear()
        eraserPath.clear()

        EditorState.notifyErasingEnded()
    }

    /**
     * Process eraser movement during drawing (collect points only, no erasing)
     * @param touchPoint Touch point from eraser movement
     */
    fun processEraserMovement(touchPoint: TouchPoint?) {
        Log.d(TAG, "Collecting eraser movement point: $touchPoint")
        touchPoint?.let { point ->
            eraserPath.add(point)
        }
    }

    /**
     * Process complete eraser path when user stops erasing
     * This is the only place where actual erasing occurs
     * @param touchPointList Complete path of eraser movement
     */
    fun processCompleteEraserPath(touchPointList: TouchPointList?) {
        touchPointList?.let { eraserPathList ->
            Log.d(TAG, "Processing complete eraser path with ${eraserPathList.size()} points")

            // Add all points to eraser path
            eraserPath.addAll(eraserPathList.points)

            // Adjust eraser path for viewport transforms if controller is available
            val adjustedEraserPath = viewportController?.let { controller ->
                adjustEraserPathForViewport(eraserPathList, controller)
            } ?: eraserPathList

            // Find shapes to erase using the complete adjusted path
            val availableShapes = shapeManager.getShapesSnapshot()
            val shapesToErase = EraserUtils.findShapesToErase(adjustedEraserPath, availableShapes)

            if (shapesToErase.isNotEmpty()) {
                eraseShapesCompletely(shapesToErase)
                Log.d(TAG, "Erased ${shapesToErase.size} shapes with complete path")
            } else {
                Log.d(TAG, "No shapes to erase with complete path")
            }
        }
    }

    /**
     * Get the current erasing session for database operations
     * @return Current erasing session
     */
    fun getCurrentErasingSession(): EraserUtils.ErasingSession = currentErasingSession

    /**
     * Erase shapes completely at the end of erasing session
     * @param shapesToErase Collection of shapes to erase
     */
    private fun eraseShapesCompletely(shapesToErase: Collection<com.wyldsoft.notes.editorview.drawing.shape.DrawingShape>) {
        Log.d(TAG, "Erasing ${shapesToErase.size} shapes completely")
        
        // Remove shapes from shape manager
        val removedCount = shapeManager.removeShapes(shapesToErase)
        Log.d(TAG, "Removed $removedCount shapes from shape manager")

        // Add to erasing session
        shapesToErase.forEach { shape ->
            currentErasingSession.addErasedShape(shape)
        }

        // Note: Database update and refresh will happen in endErasing()
    }

    /**
     * Update database with remaining shapes after erasing
     */
    private fun updateDatabaseWithRemainingShapes() {
        val remainingShapes = shapeManager.getAllShapes()
        
        // Get current pen profile from the provided function
        val currentPenProfile = getPenProfile()
        
        Log.d(TAG, "Updating database with ${remainingShapes.size} remaining shapes")
        databaseManager.updateDatabaseAfterErasing(remainingShapes, currentPenProfile)
    }

    /**
     * Perform optimized partial refresh for erased areas
     * @param surfaceView Optional surface view for refresh
     */
    private fun performOptimizedRefresh(surfaceView: android.view.SurfaceView?) {
        // Initialize partial refresh manager if not already done
        if (!::partialRefreshManager.isInitialized) {
            partialRefreshManager = PartialRefreshEraserManager(
                getRxManager(), 
                getRendererHelper(), 
                viewportController
            )
        }
        
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
     * Adjust eraser path to account for viewport transforms (zoom and scroll)
     * Converts screen coordinates to canvas coordinates at 100% zoom
     * @param eraserPath Original eraser path from input
     * @param viewportController Controller with current viewport state
     * @return Adjusted eraser path in canvas coordinates
     */
    private fun adjustEraserPathForViewport(
        eraserPath: TouchPointList,
        viewportController: com.wyldsoft.notes.editorview.viewport.ViewportController
    ): TouchPointList {
        val originalPoints = eraserPath.points
        if (originalPoints.isNullOrEmpty()) {
            return eraserPath
        }

        val adjustedPoints = originalPoints.map { touchPoint ->
            // Convert screen coordinates to canvas coordinates
            val screenPoint = PointF(touchPoint.x, touchPoint.y)
            val canvasPoint = viewportController.screenToCanvas(screenPoint)
            
            TouchPoint().apply {
                x = canvasPoint.x
                y = canvasPoint.y
                pressure = touchPoint.pressure
                timestamp = touchPoint.timestamp
                size = touchPoint.size
            }
        }

        // Create new TouchPointList with adjusted points
        val adjustedEraserPath = TouchPointList()
        adjustedPoints.forEach { point ->
            adjustedEraserPath.add(point)
        }

        return adjustedEraserPath
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