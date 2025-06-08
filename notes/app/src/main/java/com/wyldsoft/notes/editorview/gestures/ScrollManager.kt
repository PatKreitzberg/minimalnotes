package com.wyldsoft.notes.editorview.gestures

import com.wyldsoft.notes.editorview.viewport.ViewportController
import kotlin.math.abs

/**
 * Manages scrolling operations by translating touch gestures into viewport scroll commands
 */
class ScrollManager(
    private val onScrollEvent: (String) -> Unit
) {
    private var viewportController: ViewportController? = null
    private var isScrolling = false
    private var onForceRefresh: (() -> Unit)? = null
    
    // Configuration for scroll sensitivity (1.0 = 1:1 pixel mapping)
    private val scrollSensitivity = 1.0f
    
    /**
     * Set the viewport controller for performing scroll operations
     */
    fun setViewportController(controller: ViewportController?) {
        this.viewportController = controller
    }
    
    /**
     * Set callback for forcing screen refresh
     */
    fun setForceRefreshCallback(callback: (() -> Unit)?) {
        this.onForceRefresh = callback
    }
    
    /**
     * Start a scrolling session
     */
    fun startScrolling() {
        isScrolling = true
    }
    
    /**
     * Apply proportional scrolling based on delta movements
     * Now uses direct pixel-to-pixel mapping for smooth, proportional scrolling
     */
    fun applyScroll(deltaX: Float, deltaY: Float) {
        if (!isScrolling) return
        
        val controller = viewportController ?: return
        
        // Apply proportional scrolling with sensitivity adjustment
        val adjustedDeltaX = deltaX * scrollSensitivity
        val adjustedDeltaY = deltaY * scrollSensitivity
        
        // Use direct pixel scrolling for smooth, proportional movement
        val scrolled = controller.scrollByPixels(adjustedDeltaX, adjustedDeltaY)
        
        // Force a screen refresh to ensure visual update
        if (scrolled) {
            onForceRefresh?.invoke()
        }
        
        // Notify about scroll event
        val gesture = "Scrolled proportionally by (${adjustedDeltaX.toInt()}, ${adjustedDeltaY.toInt()})"
        onScrollEvent(gesture)
    }
    
    /**
     * Stop the current scrolling session
     */
    fun stopScrolling() {
        isScrolling = false
    }
    
    /**
     * Check if currently scrolling
     */
    fun isScrolling(): Boolean = isScrolling
    
    /**
     * Reset the scroll manager state
     */
    fun reset() {
        isScrolling = false
    }
}