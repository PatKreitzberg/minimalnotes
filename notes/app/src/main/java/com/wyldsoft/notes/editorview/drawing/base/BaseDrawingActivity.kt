package com.wyldsoft.notes.editorview.drawing.base

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.graphics.createBitmap
import com.wyldsoft.notes.base.BaseDeviceReceiver
import com.wyldsoft.notes.editorview.editor.EditorState
import com.wyldsoft.notes.editorview.editor.EditorView
import com.wyldsoft.notes.editorview.gestures.GestureDetector
import com.wyldsoft.notes.editorview.viewport.ViewportController
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.pen.PenType
import com.wyldsoft.notes.ui.theme.MinimaleditorTheme

/**
 * Refactored BaseDrawingActivity with consolidated touch helper update methods
 * REFACTORED: Simplified the touch helper update interface
 */
abstract class BaseDrawingActivity : ComponentActivity() {
    protected val TAG = "BaseDrawingActivity"

    // Common drawing state
    protected var paint = Paint()
    protected var bitmap: Bitmap? = null
    protected var bitmapCanvas: Canvas? = null
    open var surfaceView: SurfaceView? = null
    protected var isDrawingInProgress = false
    var currentPenProfile = PenProfile.getDefaultProfile(PenType.BALLPEN)
    
    // Gesture detection
    protected var gestureDetector: GestureDetector? = null
    protected var gestureHandler: com.wyldsoft.notes.editorview.gestures.GestureHandler? = null

    // Abstract methods that must be implemented by SDK-specific classes
    abstract fun initializeSDK()
    abstract fun createTouchHelper(surfaceView: SurfaceView): BaseTouchHelper
    abstract fun createDeviceReceiver(): BaseDeviceReceiver
    abstract fun enableFingerTouch()
    abstract fun disableFingerTouch()
    abstract fun cleanSurfaceView(surfaceView: SurfaceView): Boolean
    abstract fun renderToScreen(surfaceView: SurfaceView, bitmap: Bitmap?)

    // Template methods - common implementation for all SDKs
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeSDK()
        initializePaint()
        initializeDeviceReceiver()

        setContent {
            MinimaleditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorView(
                        onSurfaceViewCreated = { sv ->
                            handleSurfaceViewCreated(sv)
                        },
                        onPenProfileChanged = { penProfile ->
                            updatePenProfile(penProfile)
                        }
                    )
                }
            }
        }

        EditorState.setMainActivity(this as com.wyldsoft.notes.MainActivity)
    }

    override fun onResume() {
        super.onResume()
        onResumeDrawing()
    }

    override fun onPause() {
        super.onPause()
        onPauseDrawing()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupResources()
    }

    // Common functionality
    private fun initializePaint() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        updatePaintFromProfile()
    }

    protected fun updatePaintFromProfile() {
        paint.color = currentPenProfile.getColorAsInt()
        paint.strokeWidth = currentPenProfile.strokeWidth
        Log.d(TAG, "Updated paint: color=${currentPenProfile.strokeColor}, width=${currentPenProfile.strokeWidth}")
    }

    open fun handleSurfaceViewCreated(sv: SurfaceView) {
        surfaceView = sv
        initializeTouchHelper(sv)
    }

    protected open fun initializeTouchHelper(surfaceView: SurfaceView) {
        val touchHelper = createTouchHelper(surfaceView)
        
        // Initialize gesture detection
        initializeGestureDetection(surfaceView)

        surfaceView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateActiveSurface()
        }

        val surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                cleanSurfaceView(surfaceView)
                bitmap?.let { renderToScreen(surfaceView, it) }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                updateActiveSurface()
                bitmap?.let { renderToScreen(surfaceView, it) }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                holder.removeCallback(this)
            }
        }
        surfaceView.holder.addCallback(surfaceCallback)
    }
    
    /**
     * Initialize gesture detection system
     */
    @SuppressLint("ClickableViewAccessibility")
    protected open fun initializeGestureDetection(surfaceView: SurfaceView) {
        gestureHandler = com.wyldsoft.notes.editorview.gestures.GestureHandler()

        gestureDetector = GestureDetector(this) { gesture: String ->
            Log.d(TAG, gesture)
            Log.d(TAG, "Gesture detected: $gesture")
            gestureHandler?.handleGesture(gesture)
        }

        Log.d(TAG, "Gesture detection initialized for surface view: ${surfaceView.id}")

        // Set up touch event handling for gestures
        surfaceView.setOnTouchListener { _, event ->
            val gestureConsumed = gestureDetector?.onTouchEvent(event) ?: false
            Log.d(TAG, "Gesture consumed: $gestureConsumed, action=${event.actionMasked}, pointerCount=${event.pointerCount}")

            // If a gesture was detected, don't pass the event to drawing
            if (gestureConsumed) {
                true
            } else {
                // Let the original touch handling proceed
                false
            }
        }
    }
    
    /**
     * Set the viewport controller for gesture-based navigation
     * This should be called after the ViewportController is available
     */
    protected open fun setGestureViewportController(viewportController: ViewportController?) {
        gestureDetector?.setViewportController(viewportController)
        Log.d(TAG, "Gesture viewport controller set: ${viewportController != null}")
    }

    /**
     * Update pen profile and trigger touch helper reconfiguration
     * REFACTORED: Now delegates to updateTouchHelperWithProfile for consistency
     */
    open fun updatePenProfile(penProfile: PenProfile) {
        Log.d(TAG, "Updating pen profile: $penProfile")
        currentPenProfile = penProfile
        updatePaintFromProfile()
        updateTouchHelperWithProfile()
    }

    /**
     * Update exclusion zones and trigger touch helper reconfiguration
     * REFACTORED: Simplified interface - implementations should handle the details
     */
    open fun updateExclusionZones(excludeRects: List<Rect>) {
        updateTouchHelperExclusionZones(excludeRects)
        Log.d(TAG, "Updated exclusion zones, forcing screen refresh")
        forceScreenRefresh()
    }

    protected open fun forceScreenRefresh() {
        Log.d(TAG, "forceScreenRefresh()")
        bitmapCanvas?.drawColor(Color.WHITE)
        surfaceView?.let { sv ->
            cleanSurfaceView(sv)
            bitmap?.let { renderToScreen(sv, it) }
        }
    }

    protected fun createDrawingBitmap(): Bitmap? {
        return surfaceView?.let { sv ->
            if (bitmap == null) {
                bitmap = createBitmap(sv.width, sv.height)
                bitmapCanvas = Canvas(bitmap!!)
                bitmapCanvas?.drawColor(Color.WHITE)
            }
            bitmap
        }
    }

    private fun cleanupResources() {
        onCleanupSDK()
        bitmap?.recycle()
        bitmap = null
        onCleanupDeviceReceiver()
    }

    // Abstract methods for SDK-specific lifecycle
    protected abstract fun onResumeDrawing()
    protected abstract fun onPauseDrawing()
    protected abstract fun onCleanupSDK()
    protected abstract fun updateActiveSurface()

    /**
     * REFACTORED: Single method for touch helper updates related to pen profile changes
     * Implementations should consolidate all touch helper configuration logic here
     */
    protected abstract fun updateTouchHelperWithProfile()

    /**
     * REFACTORED: Single method for touch helper updates related to exclusion zone changes
     * Implementations should consolidate all exclusion zone handling logic here
     * Note: This can delegate to the same internal method as updateTouchHelperWithProfile
     */
    protected abstract fun updateTouchHelperExclusionZones(excludeRects: List<Rect>)

    protected abstract fun initializeDeviceReceiver()
    protected abstract fun onCleanupDeviceReceiver()
}