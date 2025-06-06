package com.wyldsoft.notes.editorview.drawing.onyx

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.SurfaceView
import androidx.core.graphics.createBitmap
import com.wyldsoft.notes.render.RendererHelper
import com.wyldsoft.notes.render.RendererToScreenRequest
import com.wyldsoft.notes.editorview.drawing.shape.Shape
import com.onyx.android.sdk.rx.RxManager

/**
 * Manages bitmap creation, shape rendering, and screen refresh operations for the Onyx drawing system
 * Handles all rendering-related functionality including bitmap management and screen updates
 */
class OnyxRenderingManager {
    companion object {
        private const val TAG = "OnyxRenderingManager"
    }

    // Rendering components
    private var rendererHelper: RendererHelper? = null
    private var rxManager: RxManager? = null

    // Current bitmap and canvas for drawing
    private var currentBitmap: Bitmap? = null
    private var currentCanvas: Canvas? = null

    init {
        initializeRenderer()
    }

    /**
     * Initialize the renderer helper and RxManager
     */
    private fun initializeRenderer() {
        rendererHelper = RendererHelper()
        rxManager = RxManager.Builder.sharedSingleThreadManager()
    }

    /**
     * Get the renderer helper instance
     * @return RendererHelper for rendering operations
     */
    fun getRendererHelper(): RendererHelper {
        return rendererHelper ?: run {
            initializeRenderer()
            rendererHelper!!
        }
    }

    /**
     * Initialize surface view and create initial bitmap
     * @param surfaceView SurfaceView to initialize for
     */
    fun initializeSurfaceView(surfaceView: SurfaceView) {
        createOrGetDrawingBitmap(surfaceView)
        Log.d(TAG, "Initialized surface view with bitmap")
    }

    /**
     * Create or get the current drawing bitmap for the given surface
     * @param surfaceView SurfaceView to create bitmap for
     * @return Bitmap ready for drawing operations
     */
    fun createOrGetDrawingBitmap(surfaceView: SurfaceView?): Bitmap? {
        return surfaceView?.let { sv ->
            if (currentBitmap == null ||
                currentBitmap?.width != sv.width ||
                currentBitmap?.height != sv.height) {

                // Recycle old bitmap if it exists
                currentBitmap?.recycle()

                // Create new bitmap
                Log.d(TAG, "Creating new bitmap: ${sv.width}x${sv.height}")
                currentBitmap = createBitmap(sv.width, sv.height)
                currentCanvas = Canvas(currentBitmap!!)
                currentCanvas?.drawColor(Color.WHITE)

                Log.d(TAG, "Created new bitmap: ${sv.width}x${sv.height}")
            }
            currentBitmap
        }
    }

    /**
     * Render a single shape to the current bitmap
     * @param shape Shape to render
     */
    fun renderShapeToBitmap(shape: Shape) {
        currentBitmap?.let { bitmap ->
            val renderContext = getRendererHelper().getRenderContext()

            setupRenderContext(renderContext, bitmap)

            try {
                shape.render(renderContext)
                Log.d(TAG, "Rendered shape to bitmap")
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering shape to bitmap", e)
            }
        }
    }

    /**
     * Recreate the entire bitmap from a collection of shapes
     * @param shapes Collection of shapes to render
     */
    fun recreateBitmapFromShapes(shapes: List<Shape>) {
        currentBitmap?.let { bitmap ->
            // Clear the bitmap
            currentCanvas?.drawColor(Color.WHITE)

            // Set up render context
            val renderContext = getRendererHelper().getRenderContext()
            setupRenderContext(renderContext, bitmap)

            // Render all shapes
            shapes.forEach { shape ->
                try {
                    shape.render(renderContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering shape during bitmap recreation", e)
                }
            }

            Log.d(TAG, "Recreated bitmap from ${shapes.size} shapes")
        }
    }

    /**
     * Setup render context with current bitmap and canvas
     * @param renderContext RenderContext to configure
     * @param bitmap Bitmap to render to
     */
    private fun setupRenderContext(renderContext: RendererHelper.RenderContext, bitmap: Bitmap) {
        renderContext.bitmap = bitmap
        renderContext.canvas = currentCanvas ?: Canvas(bitmap)
        renderContext.paint = createRenderPaint()
        renderContext.viewPoint = android.graphics.Point(0, 0)
    }

    /**
     * Create paint object for rendering operations
     * @return Paint configured for shape rendering
     */
    private fun createRenderPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    /**
     * Render bitmap to screen using RxManager
     * @param surfaceView SurfaceView to render to
     * @param bitmap Bitmap to render, or null to use current bitmap
     */
    fun renderToScreen(surfaceView: SurfaceView?, bitmap: Bitmap?) {
        surfaceView ?: return

        val bitmapToRender = bitmap ?: currentBitmap
        bitmapToRender ?: return

        try {
            val renderRequest = RendererToScreenRequest(surfaceView, bitmapToRender)
            getRxManager().enqueue(renderRequest, null)
            Log.d(TAG, "Enqueued render to screen request")
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering to screen", e)
        }
    }

    /**
     * Clean the surface view (fill with white background)
     * @param surfaceView SurfaceView to clean
     * @return True if cleaning was successful
     */
    fun cleanSurfaceView(surfaceView: SurfaceView): Boolean {
        return try {
            val holder = surfaceView.holder
            if (!holder.surface.isValid) {
                return false
            }

            val canvas = holder.lockCanvas()
            canvas?.let {
                it.drawColor(Color.WHITE)
                holder.unlockCanvasAndPost(it)
                Log.d(TAG, "Successfully cleaned surface view")
                true
            } ?: false

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning surface view", e)
            false
        }
    }

    /**
     * Force a complete screen refresh
     * @param surfaceView SurfaceView to refresh
     * @param allShapes All shapes to render during refresh
     */
    fun forceScreenRefresh(surfaceView: SurfaceView?, allShapes: List<Shape>) {
        Log.d(TAG, "Forcing complete screen refresh")

        surfaceView?.let { sv ->
            // Clean the surface
            cleanSurfaceView(sv)

            // Recreate bitmap from all shapes
            createOrGetDrawingBitmap(sv)
            recreateBitmapFromShapes(allShapes)

            // Render to screen
            renderToScreen(sv, currentBitmap)
        }
    }

    /**
     * Clear the surface and bitmap
     * @param surfaceView SurfaceView to clear
     */
    fun clearSurface(surfaceView: SurfaceView?) {
        surfaceView?.let { sv ->
            // Clear the bitmap
            currentBitmap?.recycle()
            currentBitmap = null
            currentCanvas = null

            // Clean the surface view
            cleanSurfaceView(sv)

            Log.d(TAG, "Cleared surface and bitmap")
        }
    }

    /**
     * Get current bitmap (read-only access)
     * @return Current bitmap or null if none exists
     */
    fun getCurrentBitmap(): Bitmap? = currentBitmap

    /**
     * Get RxManager for rendering operations
     * @return RxManager instance
     */
    private fun getRxManager(): RxManager {
        return rxManager ?: run {
            rxManager = RxManager.Builder.sharedSingleThreadManager()
            rxManager!!
        }
    }

    /**
     * Cleanup rendering resources
     */
    fun cleanup() {
        currentBitmap?.recycle()
        currentBitmap = null
        currentCanvas = null

        Log.d(TAG, "Cleaned up rendering resources")
    }
}