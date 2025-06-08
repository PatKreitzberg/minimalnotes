package com.wyldsoft.notes.editorview.gestures

import android.content.Context
import android.os.CountDownTimer
import android.view.MotionEvent
import androidx.compose.ui.unit.dp
import com.wyldsoft.notes.editorview.viewport.ViewportController
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Unified gesture detector that follows the exact logic of the working oldGestureDetector
 * Detects swipes, taps, and pinch gestures and performs actual viewport transformations
 */
class GestureDetector(
    context: Context,
    private val onGestureDetected: (String) -> Unit
) {
    // Viewport controller for performing actual scroll/zoom operations
    private var viewportController: ViewportController? = null
    // Minimum distance required for a swipe gesture in dp
    private val SWIPE_THRESHOLD_DP = 50.dp
    private val SCROLL_THRESHOLD = GestureUtils.convertDpToPixel(10.dp, context)
    
    // Minimum velocity required for a swipe gesture
    private val SWIPE_VELOCITY_THRESHOLD = 100
    
    // Time window for allowing all fingers to make contact (in ms)
    private val FINGER_CONTACT_WINDOW = 150L
    
    // Time window for double tap detection (in ms)
    private val TAP_TIMEOUT = 300L
    
    // Convert dp to pixels for the current context
    private val swipeThreshold = GestureUtils.convertDpToPixel(SWIPE_THRESHOLD_DP, context)
    private var tapTimer: CountDownTimer? = null
    
    // Track gesture state
    private var isInGesture = false
    private var gestureStartTime = 0L
    private var maxPointerCount = 0
    private var tapCount = 0
    private var countedFirstTap = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isScrolling = false
    private var initialX = 0f
    private var initialY = 0f
    
    // Custom pinch-to-zoom tracking variables
    private var isZooming = false
    private var initialDistance = 0f
    private var activePointerId1 = -1
    private var activePointerId2 = -1
    private var focusX = 0f
    private var focusY = 0f

    /**
     * Check if the event is from a stylus rather than a finger.
     */
    private fun isStylusEvent(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS) {
                return true
            }
        }
        return false
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
     * Handle the start of a pinch gesture
     */
    private fun startPinch(event: MotionEvent) {
        if (event.pointerCount != 2) return

        isZooming = true
        activePointerId1 = event.getPointerId(0)
        activePointerId2 = event.getPointerId(1)

        val pointerIndex1 = event.findPointerIndex(activePointerId1)
        val pointerIndex2 = event.findPointerIndex(activePointerId2)

        initialDistance = getDistance(event, pointerIndex1, pointerIndex2)
        val (x, y) = getFocusPoint(event, pointerIndex1, pointerIndex2)
        focusX = x
        focusY = y
    }

    /**
     * Handle pinch movement with continuous zooming
     */
    private fun handlePinch(event: MotionEvent) {
        if (!isZooming) return

        val pointerIndex1 = event.findPointerIndex(activePointerId1)
        val pointerIndex2 = event.findPointerIndex(activePointerId2)

        if (pointerIndex1 == -1 || pointerIndex2 == -1) {
            isZooming = false
            return
        }

        val currentDistance = getDistance(event, pointerIndex1, pointerIndex2)
        val (x, y) = getFocusPoint(event, pointerIndex1, pointerIndex2)
        focusX = x
        focusY = y

        // Calculate continuous zoom scale based on distance change
        if (initialDistance > 0) {
            val scaleFactor = currentDistance / initialDistance
            val currentZoom = viewportController?.getZoomLevel() ?: 1.0f
            
            // Apply zoom transformation based on scale factor
            applyZooming(scaleFactor, focusX, focusY)
            
            // Log zoom action
            val gesture = if (scaleFactor > 1.0f) {
                "Zooming in: scale factor ${String.format("%.2f", scaleFactor)} (center: ${focusX.toInt()}, ${focusY.toInt()})"
            } else {
                "Zooming out: scale factor ${String.format("%.2f", scaleFactor)} (center: ${focusX.toInt()}, ${focusY.toInt()})"
            }
            onGestureDetected(gesture)
            
            // Update initial distance for next calculation
            initialDistance = currentDistance
        }
    }

    /**
     * End the pinch gesture
     */
    private fun endPinch() {
        if (isZooming) {
            isZooming = false
            activePointerId1 = -1
            activePointerId2 = -1
        }
    }

    private fun xyDistance(x: Float, y: Float): Float {
        return sqrt((x * x) + (y * y))
    }

    /**
     * Perform continuous scrolling based on movement deltas
     */
    private fun performScrolling(deltaX: Float, deltaY: Float) {
        // Apply continuous scrolling through viewport controller
        applyScrolling(deltaX, deltaY)
        
        // Log the scrolling action for debugging
        val distance = xyDistance(deltaX, deltaY)
        if (distance > swipeThreshold) {
            val gesture = if (abs(deltaX) > abs(deltaY)) {
                // Horizontal scroll
                if (deltaX > 0) {
                    "Scrolling left (deltaX: $deltaX)"
                } else {
                    "Scrolling right (deltaX: $deltaX)"
                }
            } else {
                // Vertical scroll  
                if (deltaY > 0) {
                    "Scrolling up (deltaY: $deltaY)"
                } else {
                    "Scrolling down (deltaY: $deltaY)"
                }
            }
            onGestureDetected(gesture)
        }
    }

    /**
     * Process touch events to detect gestures - following oldGestureDetector logic exactly
     */
    fun onTouchEvent(event: MotionEvent): Boolean {


        // Check if this is a stylus input - if so, ignore for gesture detection
        if (isStylusEvent(event)) {
            return false
        }

        val action = event.actionMasked
        val currentTime = System.currentTimeMillis()

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Start pinch when second pointer is down
                if (event.pointerCount == 2) {
                    startPinch(event)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Handle pinch movement
                if (isZooming) {
                    handlePinch(event)
                    return true
                }

                // Handle scrolling if not zooming
                if (!isZooming) {
                    val deltaX = lastTouchX - event.x
                    val deltaY = lastTouchY - event.y

                    if (!isScrolling) {
                        if (xyDistance(deltaX, deltaY) > SCROLL_THRESHOLD) {
                            isScrolling = true
                            // Start continuous scrolling
                            performScrolling(deltaX, deltaY)
                        }
                    } else {
                        // Continue scrolling
                        performScrolling(deltaX, deltaY)
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Check if one of our active pointers is going up
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == activePointerId1 || pointerId == activePointerId2) {
                    endPinch()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // End any ongoing pinch
                endPinch()
                actionUpOrActionCancel(event, action == MotionEvent.ACTION_UP, currentTime)
            }

            MotionEvent.ACTION_DOWN -> {
                // First finger down - start tracking a new gesture
                lastTouchX = event.x
                initialX = event.x
                lastTouchY = event.y
                initialY = event.y
                
                if (!isInGesture) {
                    gestureStartTime = currentTime
                    tapCount++
                    countedFirstTap = true
                } else {
                    // Could be single finger triple tap
                    stopTapTimer()
                }

                isInGesture = true
                maxPointerCount = 1
            }
        }

        return isInGesture || isZooming
    }

    private fun stopTapTimer() {
        tapTimer?.cancel()
    }

    private fun startTapTimer() {
        tapTimer?.cancel()
        tapTimer = object : CountDownTimer(TAP_TIMEOUT, 100) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                if (tapCount == 2) {
                    handleMultiFingerDoubleTap(maxPointerCount)
                } else if (tapCount > 2) {
                    handleMultiFingerTripleTap(maxPointerCount)
                }
                resetMultiTap()
            }
        }.start()
    }

    private fun actionUpOrActionCancel(event: MotionEvent, isActionUp: Boolean, currentTime: Long) {
        isScrolling = false
        
        if (isInGesture) {
            val timeNow = System.currentTimeMillis()
            val isNotQuickTap = timeNow - gestureStartTime > FINGER_CONTACT_WINDOW
            
            if (isNotQuickTap) {
                tapCount++
                startTapTimer()
            }
        }
    }

    private fun resetMultiTap() {
        tapCount = 0
        isInGesture = false
        maxPointerCount = 0
        countedFirstTap = false
    }

    private fun handleMultiFingerDoubleTap(fingerCount: Int) {
        val gesture = when (fingerCount) {
            1 -> "Gesture detected: Single-finger double tap"
            2 -> "Gesture detected: Two-finger double tap"
            3 -> "Gesture detected: Three-finger double tap"
            4 -> "Gesture detected: Four-finger double tap"
            else -> "Gesture detected: Multi-finger double tap"
        }
        onGestureDetected(gesture)
    }

    private fun handleMultiFingerTripleTap(fingerCount: Int) {
        val gesture = when (fingerCount) {
            1 -> "Gesture detected: Single-finger triple tap"
            2 -> "Gesture detected: Two-finger triple tap"
            3 -> "Gesture detected: Three-finger triple tap"
            4 -> "Gesture detected: Four-finger triple tap"
            else -> "Gesture detected: Multi-finger triple tap"
        }
        onGestureDetected(gesture)
    }

    /**
     * Set the viewport controller for performing actual viewport operations
     */
    fun setViewportController(controller: ViewportController?) {
        this.viewportController = controller
    }
    
    /**
     * Apply scrolling through discrete viewport controller operations
     */
    private fun applyScrolling(deltaX: Float, deltaY: Float) {
        val controller = viewportController ?: return
        
        // Convert pixel deltas to discrete scroll steps
        val scrollStepSize = 50f // Smaller steps for smoother scrolling
        
        val horizontalSteps = (deltaX / scrollStepSize).toInt()
        val verticalSteps = (deltaY / scrollStepSize).toInt()
        
        // Apply horizontal scrolling
        repeat(kotlin.math.abs(horizontalSteps)) {
            if (horizontalSteps > 0) {
                controller.scrollRight()
            } else {
                controller.scrollLeft()
            }
        }
        
        // Apply vertical scrolling
        repeat(kotlin.math.abs(verticalSteps)) {
            if (verticalSteps > 0) {
                controller.scrollDown()
            } else {
                controller.scrollUp()
            }
        }
    }
    
    /**
     * Apply zooming through discrete viewport controller operations
     */
    private fun applyZooming(scaleFactor: Float, focusX: Float, focusY: Float) {
        val controller = viewportController ?: return
        
        // Apply zoom based on scale factor
        if (scaleFactor > 1.05f && controller.canZoomIn()) {
            controller.zoomIn()
        } else if (scaleFactor < 0.95f && controller.canZoomOut()) {
            controller.zoomOut()
        }
    }
    
    fun reset() {
        isInGesture = false
        isScrolling = false
        isZooming = false
        tapCount = 0
        maxPointerCount = 0
        stopTapTimer()
    }
}