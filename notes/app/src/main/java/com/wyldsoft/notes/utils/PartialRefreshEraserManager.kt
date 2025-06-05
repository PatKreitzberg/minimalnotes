package com.wyldsoft.notes.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.SurfaceView
import androidx.core.graphics.createBitmap
import com.wyldsoft.notes.PartialRefreshRequest
import com.wyldsoft.notes.render.RendererHelper
import com.wyldsoft.notes.shapepkg.Shape
import com.onyx.android.sdk.rx.RxManager

/**
 * Manager class for handling partial refresh during erasing operations
 * Optimizes screen updates by only refreshing areas affected by erasing
 */
class PartialRefreshEraserManager(
    private val rxManager: RxManager,
    private val rendererHelper: RendererHelper
) {

    companion object {
        private const val TAG = "PartialRefreshEraserManager"
        private const val MIN_REFRESH_AREA = 100f // Minimum area to warrant partial refresh
    }

    /**
     * Perform a partial refresh of the screen for the given area
     * @param surfaceView The surface view to refresh
     * @param refreshBounds The area that needs to be refreshed
     * @param allShapes All shapes that should be rendered in the refresh area
     */
    fun performPartialRefresh(
        surfaceView: SurfaceView?,
        refreshBounds: RectF,
        allShapes: List<Shape>
    ) {
        surfaceView ?: return

        Log.d(TAG, "Performing partial refresh for bounds: $refreshBounds")

        try {
            // Validate refresh bounds
            val validatedBounds = EraserUtils.validateRefreshBounds(
                refreshBounds,
                surfaceView.width,
                surfaceView.height
            )

            // Check if partial refresh is worthwhile
            val refreshArea = validatedBounds.width() * validatedBounds.height()
            val totalArea = surfaceView.width.toFloat() * surfaceView.height.toFloat()

            if (refreshArea < MIN_REFRESH_AREA || refreshArea > totalArea * 0.5f) {
                Log.d(TAG, "Partial refresh not efficient, falling back to full refresh")
                performFullRefresh(surfaceView, allShapes)
                return
            }

            // Create bitmap for the refresh area
            val refreshWidth = validatedBounds.width().toInt()
            val refreshHeight = validatedBounds.height().toInt()

            if (refreshWidth <= 0 || refreshHeight <= 0) {
                Log.w(TAG, "Invalid refresh dimensions: ${refreshWidth}x${refreshHeight}")
                return
            }

            val refreshBitmap = createBitmap(refreshWidth, refreshHeight)
            val refreshCanvas = Canvas(refreshBitmap)

            // Clear the refresh area with white background
            refreshCanvas.drawColor(Color.WHITE)

            // Set up render context for the refresh area
            val renderContext = rendererHelper.getRenderContext()
            renderContext.bitmap = refreshBitmap
            renderContext.canvas = refreshCanvas
            renderContext.paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            renderContext.viewPoint = android.graphics.Point(
                -validatedBounds.left.toInt(),
                -validatedBounds.top.toInt()
            )

            // Filter and render only shapes that intersect with refresh area
            val shapesToRender = filterShapesInBounds(allShapes, validatedBounds)
            Log.d(TAG, "Rendering ${shapesToRender.size} shapes in refresh area")

            // Render shapes with offset for the refresh area
            renderShapesWithOffset(shapesToRender, renderContext, validatedBounds)

            // Execute partial refresh request
            val refreshRequest = PartialRefreshRequest(
                surfaceView.context,
                surfaceView,
                validatedBounds
            ).setBitmap(refreshBitmap)

            rxManager.enqueue(refreshRequest, null)

            Log.d(TAG, "Partial refresh completed for area: $validatedBounds")

        } catch (e: Exception) {
            Log.e(TAG, "Error during partial refresh, falling back to full refresh", e)
            performFullRefresh(surfaceView, allShapes)
        }
    }

    /**
     * Fallback to full screen refresh when partial refresh is not suitable
     */
    private fun performFullRefresh(surfaceView: SurfaceView, allShapes: List<Shape>) {
        Log.d(TAG, "Performing full screen refresh")

        try {
            // Create full-size bitmap
            val fullBitmap = createBitmap(surfaceView.width, surfaceView.height)
            val fullCanvas = Canvas(fullBitmap)
            fullCanvas.drawColor(Color.WHITE)

            // Set up render context for full screen
            val renderContext = rendererHelper.getRenderContext()
            renderContext.bitmap = fullBitmap
            renderContext.canvas = fullCanvas
            renderContext.paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            renderContext.viewPoint = android.graphics.Point(0, 0)

            // Render all shapes
            allShapes.forEach { shape ->
                shape.render(renderContext)
            }

            // Use existing full refresh mechanism
            val refreshRequest = com.wyldsoft.notes.render.RendererToScreenRequest(surfaceView, fullBitmap)
            rxManager.enqueue(refreshRequest, null)

        } catch (e: Exception) {
            Log.e(TAG, "Error during full refresh", e)
        }
    }

    /**
     * Filter shapes that intersect with the refresh bounds
     */
    private fun filterShapesInBounds(shapes: List<Shape>, bounds: RectF): List<Shape> {
        return shapes.filter { shape ->
            shape.updateShapeRect()
            shape.boundingRect?.let { shapeBounds ->
                RectF.intersects(bounds, shapeBounds)
            } ?: false
        }
    }

    /**
     * Render shapes with appropriate offset for the refresh area
     */
    private fun renderShapesWithOffset(
        shapes: List<Shape>,
        renderContext: RendererHelper.RenderContext,
        refreshBounds: RectF
    ) {
        // Save the original canvas state
        val canvas = renderContext.canvas
        canvas.save()

        try {
            // Translate canvas to account for refresh area offset
            canvas.translate(-refreshBounds.left, -refreshBounds.top)

            // Render each shape
            shapes.forEach { shape ->
                try {
                    shape.render(renderContext)
                } catch (e: Exception) {
                    Log.w(TAG, "Error rendering shape during partial refresh", e)
                }
            }
        } finally {
            // Restore canvas state
            canvas.restore()
        }
    }

    /**
     * Calculate refresh bounds from multiple sources (erased shapes + eraser path)
     */
    fun calculateCombinedRefreshBounds(
        erasedShapes: Set<Shape>,
        eraserPath: List<com.onyx.android.sdk.data.note.TouchPoint>?,
        eraserRadius: Float = 20f
    ): RectF {
        val combinedBounds = RectF()

        // Add bounds from erased shapes
        if (erasedShapes.isNotEmpty()) {
            val shapesBounds = EraserUtils.calculateShapesBounds(erasedShapes)
            if (!shapesBounds.isEmpty) {
                combinedBounds.set(shapesBounds)
            }
        }

        // Add bounds from eraser path
        eraserPath?.let { points ->
            if (points.isNotEmpty()) {
                val pathBounds = EraserUtils.calculateTouchPointsBounds(points, eraserRadius)
                if (!pathBounds.isEmpty) {
                    if (combinedBounds.isEmpty) {
                        combinedBounds.set(pathBounds)
                    } else {
                        combinedBounds.union(pathBounds)
                    }
                }
            }
        }

        // Add padding for eraser radius
        if (!combinedBounds.isEmpty) {
            combinedBounds.inset(-eraserRadius, -eraserRadius)
        }

        return combinedBounds
    }
}