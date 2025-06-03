package com.wyldsoft.notes

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
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
import com.wyldsoft.notes.ui.theme.MinimaleditorTheme
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.RawInputCallback
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.rx.RxManager
import com.wyldsoft.notes.GlobalDeviceReceiver
import com.wyldsoft.notes.RendererToScreenRequest
import com.wyldsoft.notes.TouchUtils

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private var deviceReceiver = GlobalDeviceReceiver()
    private var rxManager: RxManager? = null
    private var touchHelper: TouchHelper? = null
    private val paint = Paint()
    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    private var surfaceView: SurfaceView? = null
    private var isDrawingInProgress = false

    private val STROKE_WIDTH = 3.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceReceiver.enable(this, true)
        initPaint()
        initReceiver()

        setContent {
            MinimaleditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorView(
                        onSurfaceViewCreated = { sv ->
                            surfaceView = sv
                            initTouchHelper(sv)
                        }
                    )
                }
            }
        }

        // Set reference to this activity in EditorState
        EditorState.setMainActivity(this)
    }

    override fun onResume() {
        super.onResume()
        touchHelper?.setRawDrawingEnabled(true)
    }

    override fun onPause() {
        super.onPause()
        touchHelper?.setRawDrawingEnabled(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        touchHelper?.closeRawDrawing()
        bitmap?.recycle()
        bitmap = null
        deviceReceiver.enable(this, false)
    }

    private fun getRxManager(): RxManager {
        if (rxManager == null) {
            rxManager = RxManager.Builder.sharedSingleThreadManager()
        }
        return rxManager!!
    }

    private fun renderToScreen(surfaceView: SurfaceView, bitmap: Bitmap?) {
        if (bitmap != null) {
            getRxManager().enqueue(RendererToScreenRequest(surfaceView, bitmap), null)
        }
    }

    private fun initPaint() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = STROKE_WIDTH
    }

    private fun initTouchHelper(surfaceView: SurfaceView) {
        touchHelper = TouchHelper.create(surfaceView, callback)

        surfaceView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            updateActiveSurface()
        }

        val surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                cleanSurfaceView(surfaceView)
                // Redraw bitmap content if it exists
                bitmap?.let {
                    renderToScreen(surfaceView, it)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                updateActiveSurface()
                // Redraw bitmap content after surface change
                bitmap?.let {
                    renderToScreen(surfaceView, it)
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                holder.removeCallback(this)
            }
        }
        surfaceView.holder.addCallback(surfaceCallback)
    }

    private fun updateActiveSurface() {
        touchHelper?.let { helper ->
            helper.setRawDrawingEnabled(false)
            helper.closeRawDrawing()

            val limit = Rect()
            surfaceView?.getLocalVisibleRect(limit)

            // Set up exclusion zones (this will be updated by the panel)
            val excludeRects = arrayListOf<Rect>()

            helper.setStrokeWidth(STROKE_WIDTH)
                .setLimitRect(limit, excludeRects)
                .openRawDrawing()
            helper.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)

            helper.setRawDrawingEnabled(true)
            helper.setRawDrawingRenderEnabled(true)
        }
    }

    fun updateExclusionZones(excludeRects: List<Rect>) {
        touchHelper?.let { helper ->
            helper.setRawDrawingEnabled(false)
            helper.closeRawDrawing()

            val limit = Rect()
            surfaceView?.getLocalVisibleRect(limit)

            helper.setStrokeWidth(STROKE_WIDTH)
                .setLimitRect(limit, ArrayList(excludeRects))
                .openRawDrawing()
            helper.setStrokeStyle(TouchHelper.STROKE_STYLE_FOUNTAIN)

            helper.setRawDrawingEnabled(true)
            helper.setRawDrawingRenderEnabled(true)
        }

        // Force screen refresh after exclusion zone changes
        forceScreenRefresh()
    }

    private fun forceScreenRefresh() {
        surfaceView?.let { sv ->
            // Clean the surface
            cleanSurfaceView(sv)
            // Redraw bitmap if it exists
            bitmap?.let {
                renderToScreen(sv, it)
            }
        }
    }

    private fun initReceiver() {
        deviceReceiver.setSystemNotificationPanelChangeListener { open ->
            touchHelper?.setRawDrawingEnabled(!open)
            surfaceView?.let { sv ->
                renderToScreen(sv, bitmap)
            }
        }.setSystemScreenOnListener {
            surfaceView?.let { sv ->
                renderToScreen(sv, bitmap)
            }
        }
    }

    private fun cleanSurfaceView(surfaceView: SurfaceView): Boolean {
        val holder = surfaceView.holder ?: return false
        val canvas = holder.lockCanvas() ?: return false
        canvas.drawColor(Color.RED)  // Changed to red for visibility
        holder.unlockCanvasAndPost(canvas)
        return true
    }

    private val callback = object : RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            Log.d(TAG, "onBeginRawDrawing")
            isDrawingInProgress = true
            TouchUtils.disableFingerTouch(applicationContext)

            // Notify that drawing has started (close stroke options panel)
            EditorState.notifyDrawingStarted()
        }

        override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            Log.d(TAG, "onEndRawDrawing")
            isDrawingInProgress = false
            TouchUtils.enableFingerTouch(applicationContext)

            // Force refresh after drawing ends
            forceScreenRefresh()

            // Notify that drawing has ended
            EditorState.notifyDrawingEnded()
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {
            Log.d(TAG, "onRawDrawingTouchPointMoveReceived")
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList?) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived")
            touchPointList?.points?.let { points ->
                if (!isDrawingInProgress) {
                    isDrawingInProgress = true
                }
                drawScribbleToBitmap(points)
            }
        }

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint?) {
            Log.d(TAG, "onBeginRawErasing")
        }

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint?) {
            Log.d(TAG, "onEndRawErasing")
        }

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {
            Log.d(TAG, "onRawErasingTouchPointMoveReceived")
        }

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {
            Log.d(TAG, "onRawErasingTouchPointListReceived")
        }
    }

    private fun drawScribbleToBitmap(points: List<TouchPoint>) {
        surfaceView?.let { sv ->
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(
                    sv.width,
                    sv.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmapCanvas = Canvas(bitmap!!)
                // Fill bitmap with red background for visibility
                bitmapCanvas?.drawColor(Color.RED)
            }

            // Use fountain pen style drawing with white paint for visibility on red background
            val drawPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.WHITE  // White strokes on red background
                strokeWidth = STROKE_WIDTH
            }

            val path = Path()
            val prePoint = PointF(points[0].x, points[0].y)
            path.moveTo(prePoint.x, prePoint.y)

            for (point in points) {
                path.quadTo(prePoint.x, prePoint.y, point.x, point.y)
                prePoint.x = point.x
                prePoint.y = point.y
            }

            bitmapCanvas?.drawPath(path, drawPaint)

            // Render to screen
            renderToScreen(sv, bitmap)
        }
    }
}
