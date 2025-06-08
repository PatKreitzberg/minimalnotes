package com.wyldsoft.notes.editorview.gestures

import android.content.Context
import android.os.CountDownTimer
import android.view.MotionEvent
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Pure gesture detector that only detects gestures without performing actions
 * Delegates actual operations to ScrollManager and ZoomManager
 */
class GestureDetector(
    context: Context,
    private val onGestureDetected: (String) -> Unit
) {
    // Managers for handling specific operations
    private val scrollManager = ScrollManager(onGestureDetected)
    private val zoomManager = ZoomManager(onGestureDetected)
    // Minimum distance required for a swipe gesture in dp
    private val SCROLL_THRESHOLD = GestureUtils.convertDpToPixel(10.dp, context)
    
    // Minimum velocity required for a swipe gesture
    private val SWIPE_VELOCITY_THRESHOLD = 100
    
    // Time window for allowing all fingers to make contact (in ms)
    private val FINGER_CONTACT_WINDOW = 150L
    
    // Time window for double tap detection (in ms)
    private val TAP_TIMEOUT = 300L
    
    // Convert dp to pixels for the current context
    private var tapTimer: CountDownTimer? = null
    
    // Track gesture state
    private var isInGesture = false
    private var gestureStartTime = 0L
    private var maxPointerCount = 0
    private var tapCount = 0
    private var countedFirstTap = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialX = 0f
    private var initialY = 0f

    /**
     * Check if the event is from a stylus rather than a finger.
     */
    private fun isStylusOrEraserEvent(event: MotionEvent): Boolean {
        for (i in 0 until event.pointerCount) {
            if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(i) == MotionEvent.TOOL_TYPE_ERASER) {
                return true
            }
        }
        return false
    }

    private fun xyDistance(x: Float, y: Float): Float {
        return sqrt((x * x) + (y * y))
    }


    /**
     * Process touch events to detect gestures - following oldGestureDetector logic exactly
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        // Check if this is a stylus input - if so, ignore for gesture detection
        if (isStylusOrEraserEvent(event)) {
            return false
        }

        val action = event.actionMasked
        val currentTime = System.currentTimeMillis()

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Start pinch when second pointer is down
                if (event.pointerCount == 2) {
                    zoomManager.startPinch(event)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Handle pinch movement
                if (zoomManager.isZooming()) {
                    zoomManager.handlePinch(event)
                    return true
                }

                // Handle scrolling if not zooming
                if (!zoomManager.isZooming()) {
                    val deltaX = lastTouchX - event.x
                    val deltaY = lastTouchY - event.y

                    if (!scrollManager.isScrolling()) {
                        if (xyDistance(deltaX, deltaY) > SCROLL_THRESHOLD) {
                            scrollManager.startScrolling()
                            scrollManager.applyScroll(deltaX, deltaY)
                        }
                    } else {
                        scrollManager.applyScroll(deltaX, deltaY)
                    }
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // Check if one of our active pointers is going up
                val pointerId = event.getPointerId(event.actionIndex)
                if (zoomManager.isActivePointer(pointerId)) {
                    zoomManager.endPinch()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // End any ongoing operations
                zoomManager.endPinch()
                scrollManager.stopScrolling()
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

        return isInGesture || zoomManager.isZooming()
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
    fun setViewportController(controller: com.wyldsoft.notes.editorview.viewport.ViewportController?) {
        scrollManager.setViewportController(controller)
        zoomManager.setViewportController(controller)
    }
    
    fun reset() {
        isInGesture = false
        tapCount = 0
        maxPointerCount = 0
        stopTapTimer()
        scrollManager.reset()
        zoomManager.reset()
    }
}