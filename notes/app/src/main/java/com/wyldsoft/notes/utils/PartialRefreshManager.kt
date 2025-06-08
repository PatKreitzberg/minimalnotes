package com.wyldsoft.notes.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.SurfaceView
import androidx.core.graphics.createBitmap
import com.wyldsoft.notes.PartialRefreshRequest
import com.wyldsoft.notes.render.RendererHelper
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape
import com.wyldsoft.notes.backend.database.ShapeUtils
import com.onyx.android.sdk.rx.RxManager
import com.onyx.android.sdk.data.note.TouchPoint

/**
 * Manager class for handling partial refresh during various operations
 * Optimizes screen updates by only refreshing areas affected by drawing, erasing, moving, etc.
 */
class PartialRefreshManager(
    private val rxManager: RxManager,
    private val rendererHelper: RendererHelper,
    private val viewportController: com.wyldsoft.notes.editorview.viewport.ViewportController? = null
) {

    companion object {
        private const val TAG = "PartialRefreshManager"
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
        allShapes: List<DrawingShape>
    ) {
        surfaceView ?: return

        Log.d(TAG, "Performing partial refresh for bounds: $refreshBounds")

        try {
            // Validate refresh bounds
            val validatedBounds = RefreshUtils.validateRefreshBounds(
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
            
            // Apply viewport transformation matrix if available (critical for correct positioning)
            viewportController?.let { controller ->
                refreshCanvas.save()
                refreshCanvas.setMatrix(controller.getTransformMatrix())
                Log.d(TAG, "Applied viewport transformation to partial refresh - zoom: ${controller.getZoomLevel()}")
            }
            
            renderContext.viewPoint = android.graphics.Point(
                -validatedBounds.left.toInt(),
                -validatedBounds.top.toInt()
            )

            // Filter and render only shapes that intersect with refresh area
            val shapesToRender = ShapeUtils.filterShapesInBounds(allShapes, validatedBounds)
            Log.d(TAG, "Rendering ${shapesToRender.size} shapes in refresh area out of ${allShapes.size} total shapes")
            Log.d(TAG, "Refresh bounds: $validatedBounds")
            
            // Debug: Log some shape bounds
            shapesToRender.take(3).forEach { shape ->
                shape.updateShapeRect()
                Log.d(TAG, "Shape bounds: ${shape.boundingRect}")
            }

            // Render shapes with offset for the refresh area
            renderShapesWithOffset(shapesToRender, renderContext, validatedBounds)

            // Restore canvas state if viewport transformation was applied
            viewportController?.let {
                refreshCanvas.restore()
                Log.d(TAG, "Restored canvas state after viewport transformation")
            }

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
    private fun performFullRefresh(surfaceView: SurfaceView, allShapes: List<DrawingShape>) {
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
     * Render shapes with appropriate offset for the refresh area
     */
    private fun renderShapesWithOffset(
        shapes: List<DrawingShape>,
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
     * Calculate refresh bounds from shapes only
     * @param shapes Collection of shapes that were affected
     * @param toolRadius Radius to add as padding around shapes
     * @return Combined bounds of all shapes with padding
     */
    fun calculateRefreshBounds(
        shapes: Collection<DrawingShape>,
        toolRadius: Float = 20f
    ): RectF {
        val bounds = RefreshUtils.calculateShapesBounds(shapes)
        
        // Add padding for tool radius
        if (!bounds.isEmpty) {
            bounds.inset(-toolRadius, -toolRadius)
        }
        
        return bounds
    }

    /**
     * Calculate refresh bounds from both shapes and touch points
     * @param shapes Collection of shapes that were affected (can be null/empty)
     * @param touchPoints List of touch points from tool movement (can be null/empty)
     * @param toolRadius Radius of the tool for padding
     * @return Combined bounds from both sources with padding
     */
    fun calculateRefreshBounds(
        shapes: Collection<DrawingShape>?,
        touchPoints: List<TouchPoint>?,
        toolRadius: Float = 20f
    ): RectF {
        val boundsToUnion = mutableListOf<RectF>()
        
        // Add bounds from shapes if provided
        shapes?.let { shapeCollection ->
            if (shapeCollection.isNotEmpty()) {
                val shapesBounds = RefreshUtils.calculateShapesBounds(shapeCollection)
                if (!shapesBounds.isEmpty) {
                    boundsToUnion.add(shapesBounds)
                }
            }
        }
        
        // Add bounds from touch points if provided
        touchPoints?.let { points ->
            if (points.isNotEmpty()) {
                val pathBounds = RefreshUtils.calculateTouchPointsBounds(points, toolRadius)
                if (!pathBounds.isEmpty) {
                    boundsToUnion.add(pathBounds)
                }
            }
        }
        
        // Combine all bounds
        val combinedBounds = RefreshUtils.calculateCombinedBounds(boundsToUnion)
        
        // Add padding for tool radius if we have any bounds
        if (!combinedBounds.isEmpty) {
            combinedBounds.inset(-toolRadius, -toolRadius)
        }
        
        return combinedBounds
    }

    /**
     * Calculate refresh bounds from a list of RectF bounds
     * @param boundsList List of bounds to combine
     * @param toolRadius Additional padding to add
     * @return Combined bounds with padding
     */
    fun calculateRefreshBounds(
        boundsList: List<RectF>,
        toolRadius: Float = 20f
    ): RectF {
        val combinedBounds = RefreshUtils.calculateCombinedBounds(boundsList)
        
        // Add padding for tool radius if we have any bounds
        if (!combinedBounds.isEmpty) {
            combinedBounds.inset(-toolRadius, -toolRadius)
        }
        
        return combinedBounds
    }

    /**
     * Legacy method for backward compatibility with erasing operations
     * @param erasedShapes Set of shapes that were erased
     * @param eraserPath List of touch points from eraser movement
     * @param eraserRadius Radius of the eraser tool
     * @return Combined bounds suitable for partial refresh
     */
    @Deprecated("Use calculateRefreshBounds with Collection and List parameters instead")
    fun calculateCombinedRefreshBounds(
        erasedShapes: Set<DrawingShape>,
        eraserPath: List<TouchPoint>?,
        eraserRadius: Float = 20f
    ): RectF {
        return calculateRefreshBounds(erasedShapes, eraserPath, eraserRadius)
    }
}