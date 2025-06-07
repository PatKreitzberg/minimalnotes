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
    }

    /**
     * Update screen size when surface dimensions change
     */
    fun updateScreenSize(width: Int, height: Int) {
        viewportManager.updateScreenSize(width, height)
        notifyViewportChanged()
    }

    /**
     * Zoom operations
     */
    fun zoomIn(): Boolean {
        val changed = viewportManager.zoomIn()
        if (changed) {
            Log.d(TAG, "Zoomed in to ${viewportManager.getZoomLevel() * 100}%")
            notifyViewportChanged()
        }
        return changed
    }

    fun zoomOut(): Boolean {
        val changed = viewportManager.zoomOut()
        if (changed) {
            Log.d(TAG, "Zoomed out to ${viewportManager.getZoomLevel() * 100}%")
            notifyViewportChanged()
        }
        return changed
    }

    /**
     * Scroll operations
     */
    fun scrollUp(): Boolean {
        val changed = viewportManager.scrollUp()
        if (changed) {
            Log.d(TAG, "Scrolled up")
            notifyViewportChanged()
        }
        return changed
    }

    fun scrollDown(): Boolean {
        val changed = viewportManager.scrollDown()
        if (changed) {
            Log.d(TAG, "Scrolled down")
            notifyViewportChanged()
        }
        return changed
    }

    fun scrollLeft(): Boolean {
        val changed = viewportManager.scrollLeft()
        if (changed) {
            Log.d(TAG, "Scrolled left")
            notifyViewportChanged()
        }
        return changed
    }

    fun scrollRight(): Boolean {
        val changed = viewportManager.scrollRight()
        if (changed) {
            Log.d(TAG, "Scrolled right")
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
     * Check zoom capabilities
     */
    fun canZoomIn(): Boolean = viewportManager.canZoomIn()
    fun canZoomOut(): Boolean = viewportManager.canZoomOut()

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
        }
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