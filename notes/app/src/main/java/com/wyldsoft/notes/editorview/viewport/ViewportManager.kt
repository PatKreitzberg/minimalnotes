package com.wyldsoft.notes.editorview.viewport

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * Manages viewport transformations for scrolling and zooming functionality
 * Handles coordinate transformations between canvas and screen space
 */
class ViewportManager(
    private var screenWidth: Int,
    private var screenHeight: Int
) {
    companion object {
        const val MIN_ZOOM = 1.0f  // 100%
        const val MAX_ZOOM = 3.0f  // 300%
        const val ZOOM_STEP = 0.25f // 25%
        const val SCROLL_STEP = 100f // 100px
        const val TAG = "ViewportManager"
    }

    // Current viewport state
    private var zoomLevel: Float = MIN_ZOOM
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    // Transformation matrix for canvas drawing
    private val transformMatrix = Matrix()
    private val inverseMatrix = Matrix()

    // Viewport bounds in canvas coordinates
    private val viewportBounds = RectF()

    init {
        updateTransformMatrix()
    }

    /**
     * Update screen dimensions when surface changes
     */
    fun updateScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        constrainOffset()
        updateTransformMatrix()
    }

    /**
     * Zoom in by ZOOM_STEP, centered on viewport
     */
    fun zoomIn(): Boolean {
        val newZoom = min(zoomLevel + ZOOM_STEP, MAX_ZOOM)
        return if (newZoom != zoomLevel) {
            setZoomLevel(newZoom)
            true
        } else {
            false
        }
    }

    /**
     * Zoom out by ZOOM_STEP, centered on viewport
     */
    fun zoomOut(): Boolean {
        val newZoom = max(zoomLevel - ZOOM_STEP, MIN_ZOOM)
        return if (newZoom != zoomLevel) {
            setZoomLevel(newZoom)
            true
        } else {
            false
        }
    }

    /**
     * Set specific zoom level, centered on viewport
     */
    private fun setZoomLevel(newZoom: Float) {
        // Calculate center point in canvas coordinates before zoom
        val centerX = (screenWidth / 2f - offsetX) / zoomLevel
        val centerY = (screenHeight / 2f - offsetY) / zoomLevel

        // Update zoom level
        zoomLevel = newZoom

        // Recalculate offset to keep center point centered
        offsetX = screenWidth / 2f - centerX * zoomLevel
        offsetY = screenHeight / 2f - centerY * zoomLevel

        constrainOffset()
        updateTransformMatrix()
    }

    /**
     * Scroll in specified direction
     */
    fun scrollUp(): Boolean {
        Log.d(TAG, "Scrolling up")
        val newOffsetY = offsetY + SCROLL_STEP
        return updateOffset(offsetX, newOffsetY)
    }

    fun scrollDown(): Boolean {
        Log.d(TAG, "Scrolling down")
        val newOffsetY = offsetY - SCROLL_STEP
        return updateOffset(offsetX, newOffsetY)
    }

    fun scrollLeft(): Boolean {
        Log.d(TAG, "Scrolling left")
        val newOffsetX = offsetX + SCROLL_STEP
        return updateOffset(newOffsetX, offsetY)
    }

    fun scrollRight(): Boolean {
        val newOffsetX = offsetX - SCROLL_STEP
        return updateOffset(newOffsetX, offsetY)
    }

    /**
     * Update offset with constraints
     */
    private fun updateOffset(newOffsetX: Float, newOffsetY: Float): Boolean {
        val oldOffsetX = offsetX
        val oldOffsetY = offsetY

        offsetX = newOffsetX
        offsetY = newOffsetY
        constrainOffset()

        val changed = offsetX != oldOffsetX || offsetY != oldOffsetY
        if (changed) {
            updateTransformMatrix()
        }
        return changed
    }

    /**
     * Constrain offset to valid bounds
     */
    private fun constrainOffset() {
        // Top boundary: cannot scroll above y = 0
        val maxOffsetY = 0f
        offsetY = min(offsetY, maxOffsetY)

        // Left boundary: cannot scroll left of x = 0 at normal zoom
        val maxOffsetX = 0f
        offsetX = min(offsetX, maxOffsetX)

        // Right boundary: cannot scroll past what's visible at 100% zoom
        val minOffsetX = screenWidth - (screenWidth * zoomLevel)
        offsetX = max(offsetX, minOffsetX)

        // No bottom boundary - infinite scroll downward
    }

    /**
     * Update transformation matrices
     */
    private fun updateTransformMatrix() {
        transformMatrix.reset()
        transformMatrix.setScale(zoomLevel, zoomLevel)
        transformMatrix.postTranslate(offsetX, offsetY)

        // Update inverse matrix for coordinate conversion
        transformMatrix.invert(inverseMatrix)

        updateViewportBounds()
    }

    /**
     * Update viewport bounds in canvas coordinates
     */
    private fun updateViewportBounds() {
        val points = floatArrayOf(
            0f, 0f,  // top-left screen
            screenWidth.toFloat(), screenHeight.toFloat()  // bottom-right screen
        )
        
        inverseMatrix.mapPoints(points)
        
        viewportBounds.set(
            points[0], points[1],  // left, top
            points[2], points[3]   // right, bottom
        )
    }

    /**
     * Get transformation matrix for canvas drawing
     */
    fun getTransformMatrix(): Matrix = Matrix(transformMatrix)

    /**
     * Get current viewport bounds in canvas coordinates
     */
    fun getViewportBounds(): RectF = RectF(viewportBounds)

    /**
     * Convert screen coordinates to canvas coordinates
     */
    fun screenToCanvas(screenPoint: PointF): PointF {
        val points = floatArrayOf(screenPoint.x, screenPoint.y)
        inverseMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    /**
     * Convert canvas coordinates to screen coordinates
     */
    fun canvasToScreen(canvasPoint: PointF): PointF {
        val points = floatArrayOf(canvasPoint.x, canvasPoint.y)
        transformMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    /**
     * Get current zoom level
     */
    fun getZoomLevel(): Float = zoomLevel

    /**
     * Get current offset
     */
    fun getOffset(): PointF = PointF(offsetX, offsetY)

    /**
     * Check if zoom in is possible
     */
    fun canZoomIn(): Boolean = zoomLevel < MAX_ZOOM

    /**
     * Check if zoom out is possible
     */
    fun canZoomOut(): Boolean = zoomLevel > MIN_ZOOM
}