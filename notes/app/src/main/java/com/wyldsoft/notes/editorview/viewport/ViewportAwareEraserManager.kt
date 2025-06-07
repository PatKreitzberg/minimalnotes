package com.wyldsoft.notes.editorview.viewport

import android.graphics.RectF
import android.util.Log
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape
import com.wyldsoft.notes.utils.EraserUtils

/**
 * Viewport-aware eraser manager that only checks visible shapes for erasing
 * Optimizes erasing performance by limiting shape hit-testing to visible shapes only
 */
class ViewportAwareEraserManager(
    private val visibilityCalculator: VisibilityCalculator
) {
    companion object {
        private const val TAG = "ViewportAwareEraserManager"
    }

    /**
     * Find shapes to erase at a specific point, only checking visible shapes
     * @param point The eraser touch point
     * @param allShapes All available shapes
     * @param viewport Current viewport bounds
     * @param eraserRadius Radius of the eraser tool
     * @return List of visible shapes that intersect with the eraser point
     */
    fun findVisibleShapesToEraseAtPoint(
        point: TouchPoint,
        allShapes: List<DrawingShape>,
        viewport: RectF,
        eraserRadius: Float = 20f
    ): List<DrawingShape> {
        // Get only visible shapes first
        val visibleShapes = visibilityCalculator.getVisibleShapes(allShapes, viewport)
        
        Log.d(TAG, "Checking ${visibleShapes.size} visible shapes out of ${allShapes.size} total shapes")
        
        // Use existing EraserUtils logic on visible shapes only
        return EraserUtils.findShapesToEraseAtPoint(point, visibleShapes, eraserRadius)
    }

    /**
     * Find shapes to erase along an eraser path, only checking visible shapes
     * @param eraserPath TouchPointList representing the eraser movement
     * @param allShapes All available shapes
     * @param viewport Current viewport bounds
     * @param eraserRadius Radius of the eraser tool
     * @return List of visible shapes that intersect with the eraser path
     */
    fun findVisibleShapesToErase(
        eraserPath: TouchPointList,
        allShapes: List<DrawingShape>,
        viewport: RectF,
        eraserRadius: Float = 20f
    ): List<DrawingShape> {
        // Get only visible shapes first
        val visibleShapes = visibilityCalculator.getVisibleShapes(allShapes, viewport)
        
        Log.d(TAG, "Checking ${visibleShapes.size} visible shapes out of ${allShapes.size} total shapes for path erasing")
        
        // Use existing EraserUtils logic on visible shapes only
        return EraserUtils.findShapesToErase(eraserPath, visibleShapes, eraserRadius)
    }

    /**
     * Get performance statistics for erasing operations
     * @param allShapes All available shapes
     * @param viewport Current viewport bounds
     * @return Statistics about visible vs total shapes
     */
    fun getErasingPerformanceStats(
        allShapes: List<DrawingShape>,
        viewport: RectF
    ): ErasingPerformanceStats {
        val visibleShapes = visibilityCalculator.getVisibleShapes(allShapes, viewport)
        val visibilityStats = visibilityCalculator.getVisibilityStats(allShapes, viewport)
        
        return ErasingPerformanceStats(
            totalShapes = allShapes.size,
            visibleShapes = visibleShapes.size,
            performanceGain = if (allShapes.isNotEmpty()) {
                (1.0f - visibleShapes.size.toFloat() / allShapes.size.toFloat()) * 100f
            } else 0f,
            visibilityBounds = visibilityStats.visibleBounds
        )
    }

    /**
     * Statistics about erasing performance optimization
     */
    data class ErasingPerformanceStats(
        val totalShapes: Int,
        val visibleShapes: Int,
        val performanceGain: Float, // Percentage of shapes skipped
        val visibilityBounds: RectF
    ) {
        override fun toString(): String {
            return "ErasingStats(total=$totalShapes, visible=$visibleShapes, gain=${String.format("%.1f", performanceGain)}%)"
        }
    }
}