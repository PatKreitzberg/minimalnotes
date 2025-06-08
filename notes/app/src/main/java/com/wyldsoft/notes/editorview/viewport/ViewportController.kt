package com.wyldsoft.notes.editorview.viewport

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape

/**
 * Controls viewport operations and coordinates between ViewportManager and VisibilityCalculator
 * Provides a clean interface for the drawing system to interact with viewport functionality
 */
class ViewportController(
    screenWidth: Int,
    screenHeight: Int
) {
    companion object {
        private const val TAG = "ViewportController"
    }

    private val viewportManager = ViewportManager(screenWidth, screenHeight)
    private val visibilityCalculator = VisibilityCalculator()

    // Listeners for viewport changes
    private val viewportChangeListeners = mutableListOf<ViewportChangeListener>()

    /**
     * Interface for listening to viewport changes
     */
    interface ViewportChangeListener {
        fun onViewportChanged(viewport: RectF, zoomLevel: Float)
        fun onVisibleShapesChanged(visibleShapes: List<DrawingShape>)
        
        /**
         * Called when viewport transformation requires a full screen refresh
         * This ensures shapes are properly positioned after zoom/scroll changes
         */
        fun onViewportRefreshRequired()
    }

    /**
     * Update screen size when surface dimensions change
     */
    fun updateScreenSize(width: Int, height: Int) {
        viewportManager.updateScreenSize(width, height)
        notifyViewportChanged()
    }


    /**
     * Zoom operations centered on a specific focus point
     */
    fun zoomInAtFocus(focusX: Float, focusY: Float): Boolean {
        val currentZoom = viewportManager.getZoomLevel()
        val newZoom = kotlin.math.min(currentZoom + ViewportManager.ZOOM_STEP, ViewportManager.MAX_ZOOM)
        val changed = viewportManager.setZoomLevelAtFocus(newZoom, focusX, focusY)
        if (changed) {
            Log.d(TAG, "Zoomed in to ${viewportManager.getZoomLevel() * 100}% at focus ($focusX, $focusY)")
            notifyViewportChanged()
        }
        return changed
    }

    fun zoomOutAtFocus(focusX: Float, focusY: Float): Boolean {
        val currentZoom = viewportManager.getZoomLevel()
        val newZoom = kotlin.math.max(currentZoom - ViewportManager.ZOOM_STEP, ViewportManager.MIN_ZOOM)
        val changed = viewportManager.setZoomLevelAtFocus(newZoom, focusX, focusY)
        if (changed) {
            Log.d(TAG, "Zoomed out to ${viewportManager.getZoomLevel() * 100}% at focus ($focusX, $focusY)")
            notifyViewportChanged()
        }
        return changed
    }

    /**
     * Scroll by specific pixel amounts for proportional scrolling
     */
    fun scrollByPixels(deltaX: Float, deltaY: Float): Boolean {
        val changed = viewportManager.scrollByPixels(deltaX, deltaY)
        if (changed) {
            Log.d(TAG, "Scrolled by pixels: ($deltaX, $deltaY)")
            notifyViewportChanged()
        }
        return changed
    }

    /**
     * Get transformation matrix for canvas drawing
     */
    fun getTransformMatrix(): Matrix = viewportManager.getTransformMatrix()

    /**
     * Get current viewport bounds in canvas coordinates
     */
    fun getViewportBounds(): RectF = viewportManager.getViewportBounds()

    /**
     * Get current zoom level
     */
    fun getZoomLevel(): Float = viewportManager.getZoomLevel()

    /**
     * Convert screen coordinates to canvas coordinates
     */
    fun screenToCanvas(screenPoint: android.graphics.PointF): android.graphics.PointF {
        return viewportManager.screenToCanvas(screenPoint)
    }

    /**
     * Convert canvas coordinates to screen coordinates  
     */
    fun canvasToScreen(canvasPoint: android.graphics.PointF): android.graphics.PointF {
        return viewportManager.canvasToScreen(canvasPoint)
    }

    /**
     * Check zoom capabilities
     */
    fun canZoomIn(): Boolean = viewportManager.canZoomIn()
    fun canZoomOut(): Boolean = viewportManager.canZoomOut()

    /**
     * Reset zoom to 100%
     */
    fun resetZoomToFit(): Boolean {
        val changed = viewportManager.resetZoomToFit()
        if (changed) {
            Log.d(TAG, "Reset zoom to 100%")
            notifyViewportChanged()
        }
        return changed
    }

    /**
     * Get visible shapes from all shapes
     */
    fun getVisibleShapes(allShapes: List<DrawingShape>): List<DrawingShape> {
        val viewport = viewportManager.getViewportBounds()
        return visibilityCalculator.getVisibleShapes(allShapes, viewport)
    }

    /**
     * Update shape bounds when shapes are modified
     */
    fun updateShapeBounds(shape: DrawingShape) {
        visibilityCalculator.updateShapeBounds(shape)
    }

    /**
     * Update bounds for multiple shapes
     */
    fun updateMultipleShapeBounds(shapes: List<DrawingShape>) {
        visibilityCalculator.updateMultipleShapeBounds(shapes)
    }

    /**
     * Remove shape from visibility cache when deleted
     */
    fun removeShape(shape: DrawingShape) {
        visibilityCalculator.removeShape(shape)
    }

    /**
     * Clear all cached shape bounds (when loading new note)
     */
    fun clearShapeCache() {
        visibilityCalculator.clearCache()
    }

    /**
     * Check if a specific shape is visible
     */
    fun isShapeVisible(shape: DrawingShape): Boolean {
        val viewport = viewportManager.getViewportBounds()
        return visibilityCalculator.isShapeVisible(shape, viewport)
    }

    /**
     * Get visibility statistics for debugging
     */
    fun getVisibilityStats(allShapes: List<DrawingShape>): VisibilityCalculator.VisibilityStats {
        val viewport = viewportManager.getViewportBounds()
        return visibilityCalculator.getVisibilityStats(allShapes, viewport)
    }

    /**
     * Add listener for viewport changes
     */
    fun addViewportChangeListener(listener: ViewportChangeListener) {
        viewportChangeListeners.add(listener)
    }

    /**
     * Remove listener for viewport changes
     */
    fun removeViewportChangeListener(listener: ViewportChangeListener) {
        viewportChangeListeners.remove(listener)
    }

    /**
     * Notify all listeners of viewport changes
     */
    private fun notifyViewportChanged() {
        val viewport = viewportManager.getViewportBounds()
        val zoomLevel = viewportManager.getZoomLevel()
        
        viewportChangeListeners.forEach { listener ->
            listener.onViewportChanged(viewport, zoomLevel)
            listener.onViewportRefreshRequired()
        }
        
        Log.d(TAG, "Notified ${viewportChangeListeners.size} listeners of viewport change and refresh requirement")
    }

    /**
     * Notify listeners when visible shapes change
     */
    fun notifyVisibleShapesChanged(allShapes: List<DrawingShape>) {
        val visibleShapes = getVisibleShapes(allShapes)
        
        viewportChangeListeners.forEach { listener ->
            listener.onVisibleShapesChanged(visibleShapes)
        }
    }
}