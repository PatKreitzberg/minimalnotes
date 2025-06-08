package com.wyldsoft.notes.editorview.gestures

import android.view.MotionEvent
import com.wyldsoft.notes.editorview.viewport.ViewportController
import kotlin.math.sqrt

/**
 * Manages zoom operations by translating pinch gestures into viewport zoom commands
 */
class ZoomManager(
    private val onZoomEvent: (String) -> Unit
) {
    private var viewportController: ViewportController? = null
    
    // Pinch-to-zoom tracking variables
    private var isZooming = false
    private var initialDistance = 0f
    private var activePointerId1 = -1
    private var activePointerId2 = -1
    private var focusX = 0f
    private var focusY = 0f
    
    /**
     * Set the viewport controller for performing zoom operations
     */
    fun setViewportController(controller: ViewportController?) {
        this.viewportController = controller
    }
    
    /**
     * Calculate the distance between two pointers
     */
    private fun getDistance(event: MotionEvent, pointerIndex1: Int, pointerIndex2: Int): Float {
        val x1 = event.getX(pointerIndex1)
        val y1 = event.getY(pointerIndex1)
        val x2 = event.getX(pointerIndex2)
        val y2 = event.getY(pointerIndex2)

        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }

    /**
     * Calculate the focus point (midpoint) between two pointers
     */
    private fun getFocusPoint(event: MotionEvent, pointerIndex1: Int, pointerIndex2: Int): Pair<Float, Float> {
        val x1 = event.getX(pointerIndex1)
        val y1 = event.getY(pointerIndex1)
        val x2 = event.getX(pointerIndex2)
        val y2 = event.getY(pointerIndex2)

        return Pair((x1 + x2) / 2f, (y1 + y2) / 2f)
    }
    
    /**
     * Start a pinch gesture
     */
    fun startPinch(event: MotionEvent): Boolean {
        if (event.pointerCount != 2) return false

        isZooming = true
        activePointerId1 = event.getPointerId(0)
        activePointerId2 = event.getPointerId(1)

        val pointerIndex1 = event.findPointerIndex(activePointerId1)
        val pointerIndex2 = event.findPointerIndex(activePointerId2)

        initialDistance = getDistance(event, pointerIndex1, pointerIndex2)
        val (x, y) = getFocusPoint(event, pointerIndex1, pointerIndex2)
        focusX = x
        focusY = y
        
        return true
    }
    
    /**
     * Handle pinch movement with continuous zooming
     */
    fun handlePinch(event: MotionEvent): Boolean {
        if (!isZooming) return false

        val pointerIndex1 = event.findPointerIndex(activePointerId1)
        val pointerIndex2 = event.findPointerIndex(activePointerId2)

        if (pointerIndex1 == -1 || pointerIndex2 == -1) {
            endPinch()
            return false
        }

        val currentDistance = getDistance(event, pointerIndex1, pointerIndex2)
        val (x, y) = getFocusPoint(event, pointerIndex1, pointerIndex2)
        focusX = x
        focusY = y

        // Calculate continuous zoom scale based on distance change
        if (initialDistance > 0) {
            val scaleFactor = currentDistance / initialDistance
            
            // Apply zoom transformation based on scale factor
            applyZoom(scaleFactor, focusX, focusY)
            
            // Update initial distance for next calculation
            initialDistance = currentDistance
        }
        
        return true
    }
    
    /**
     * End the pinch gesture
     */
    fun endPinch() {
        if (isZooming) {
            isZooming = false
            activePointerId1 = -1
            activePointerId2 = -1
        }
    }
    
    /**
     * Check if a pointer is part of the active pinch
     */
    fun isActivePointer(pointerId: Int): Boolean {
        return pointerId == activePointerId1 || pointerId == activePointerId2
    }
    
    /**
     * Apply zooming through discrete viewport controller operations centered on focus point
     */
    private fun applyZoom(scaleFactor: Float, focusX: Float, focusY: Float) {
        val controller = viewportController ?: return
        
        // Apply zoom based on scale factor, centered on the focus point
        if (scaleFactor > 1.05f && controller.canZoomIn()) {
            controller.zoomInAtFocus(focusX, focusY)
            
            val gesture = "Zooming in: scale factor ${String.format("%.2f", scaleFactor)} (center: ${focusX.toInt()}, ${focusY.toInt()})"
            onZoomEvent(gesture)
        } else if (scaleFactor < 0.95f && controller.canZoomOut()) {
            controller.zoomOutAtFocus(focusX, focusY)
            
            val gesture = "Zooming out: scale factor ${String.format("%.2f", scaleFactor)} (center: ${focusX.toInt()}, ${focusY.toInt()})"
            onZoomEvent(gesture)
        }
    }
    
    /**
     * Check if currently zooming
     */
    fun isZooming(): Boolean = isZooming
    
    /**
     * Reset the zoom manager state
     */
    fun reset() {
        isZooming = false
        activePointerId1 = -1
        activePointerId2 = -1
        initialDistance = 0f
    }
}