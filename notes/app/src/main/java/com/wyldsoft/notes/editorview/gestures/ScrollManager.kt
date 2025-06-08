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
    
    // Configuration for scroll sensitivity
    private val scrollStepSize = 50f // Smaller steps for smoother scrolling
    
    /**
     * Set the viewport controller for performing scroll operations
     */
    fun setViewportController(controller: ViewportController?) {
        this.viewportController = controller
    }
    
    /**
     * Start a scrolling session
     */
    fun startScrolling() {
        isScrolling = true
    }
    
    /**
     * Apply scrolling based on delta movements
     */
    fun applyScroll(deltaX: Float, deltaY: Float) {
        if (!isScrolling) return
        
        val controller = viewportController ?: return
        
        // Convert pixel deltas to discrete scroll steps
        val horizontalSteps = (deltaX / scrollStepSize).toInt()
        val verticalSteps = (deltaY / scrollStepSize).toInt()
        
        // Apply horizontal scrolling
        repeat(abs(horizontalSteps)) {
            if (horizontalSteps > 0) {
                controller.scrollRight()
            } else {
                controller.scrollLeft()
            }
        }
        
        // Apply vertical scrolling
        repeat(abs(verticalSteps)) {
            if (verticalSteps > 0) {
                controller.scrollDown()
            } else {
                controller.scrollUp()
            }
        }
        
        // Notify about scroll event
        val gesture = "Scrolled by (${deltaX.toInt()}, ${deltaY.toInt()})"
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