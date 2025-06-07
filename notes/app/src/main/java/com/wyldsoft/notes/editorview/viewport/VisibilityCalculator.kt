package com.wyldsoft.notes.editorview.viewport

import android.graphics.RectF
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape

/**
 * Calculates which shapes are visible within the current viewport
 * Uses efficient bounding box intersection for visibility determination
 */
class VisibilityCalculator {
    
    /**
     * Data class representing shape bounds for efficient visibility checking
     */
    data class ShapeBounds(
        val shape: DrawingShape,
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    ) {
        /**
         * Check if this shape intersects with a viewport rectangle
         */
        fun intersectsViewport(viewport: RectF): Boolean {
            return !(maxX < viewport.left || 
                    minX > viewport.right || 
                    maxY < viewport.top || 
                    minY > viewport.bottom)
        }

        /**
         * Get bounding rectangle
         */
        fun getBounds(): RectF = RectF(minX, minY, maxX, maxY)
    }

    // Cache of shape bounds for efficient visibility checking
    private val shapeBoundsCache = mutableMapOf<DrawingShape, ShapeBounds>()

    /**
     * Calculate and cache bounding box for a shape
     */
    fun updateShapeBounds(shape: DrawingShape): ShapeBounds {
        // Ensure shape has updated its bounding rectangle
        shape.updateShapeRect()
        
        val bounds = shape.boundingRect ?: run {
            // Fallback: calculate from touch points if no bounds
            calculateBoundsFromTouchPoints(shape)
        }

        val shapeBounds = if (!bounds.isEmpty) {
            // Add stroke width padding
            val strokePadding = shape.strokeWidth / 2f
            ShapeBounds(
                shape = shape,
                minX = bounds.left - strokePadding,
                minY = bounds.top - strokePadding,
                maxX = bounds.right + strokePadding,
                maxY = bounds.bottom + strokePadding
            )
        } else {
            // Empty bounds for invalid shapes
            ShapeBounds(shape, 0f, 0f, 0f, 0f)
        }

        shapeBoundsCache[shape] = shapeBounds
        return shapeBounds
    }

    /**
     * Calculate bounds from touch points if shape bounds are unavailable
     */
    private fun calculateBoundsFromTouchPoints(shape: DrawingShape): RectF {
        val touchPoints = shape.touchPointList?.points ?: return RectF()
        
        if (touchPoints.isEmpty()) {
            return RectF()
        }

        var minX = touchPoints[0].x
        var maxX = touchPoints[0].x
        var minY = touchPoints[0].y
        var maxY = touchPoints[0].y

        for (point in touchPoints) {
            minX = minX.coerceAtMost(point.x)
            maxX = maxX.coerceAtLeast(point.x)
            minY = minY.coerceAtMost(point.y)
            maxY = maxY.coerceAtLeast(point.y)
        }

        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * Get all shapes that are visible within the given viewport
     */
    fun getVisibleShapes(
        allShapes: List<DrawingShape>, 
        viewport: RectF
    ): List<DrawingShape> {
        val visibleShapes = mutableListOf<DrawingShape>()

        for (shape in allShapes) {
            val shapeBounds = shapeBoundsCache[shape] ?: updateShapeBounds(shape)
            
            if (shapeBounds.intersectsViewport(viewport)) {
                visibleShapes.add(shape)
            }
        }

        return visibleShapes
    }

    /**
     * Get visible shapes with their bounds for efficient processing
     */
    fun getVisibleShapesWithBounds(
        allShapes: List<DrawingShape>,
        viewport: RectF
    ): List<ShapeBounds> {
        val visibleShapes = mutableListOf<ShapeBounds>()

        for (shape in allShapes) {
            val shapeBounds = shapeBoundsCache[shape] ?: updateShapeBounds(shape)
            
            if (shapeBounds.intersectsViewport(viewport)) {
                visibleShapes.add(shapeBounds)
            }
        }

        return visibleShapes
    }

    /**
     * Check if a specific shape is visible in the viewport
     */
    fun isShapeVisible(shape: DrawingShape, viewport: RectF): Boolean {
        val shapeBounds = shapeBoundsCache[shape] ?: updateShapeBounds(shape)
        return shapeBounds.intersectsViewport(viewport)
    }

    /**
     * Remove shape from cache when it's deleted
     */
    fun removeShape(shape: DrawingShape) {
        shapeBoundsCache.remove(shape)
    }

    /**
     * Clear all cached bounds (useful when loading new note)
     */
    fun clearCache() {
        shapeBoundsCache.clear()
    }

    /**
     * Get cached bounds for a shape, or calculate if not cached
     */
    fun getShapeBounds(shape: DrawingShape): ShapeBounds {
        return shapeBoundsCache[shape] ?: updateShapeBounds(shape)
    }

    /**
     * Update bounds for multiple shapes efficiently
     */
    fun updateMultipleShapeBounds(shapes: List<DrawingShape>) {
        shapes.forEach { shape ->
            updateShapeBounds(shape)
        }
    }

    /**
     * Get statistics about visibility calculation
     */
    fun getVisibilityStats(allShapes: List<DrawingShape>, viewport: RectF): VisibilityStats {
        var visibleCount = 0
        var totalBounds = RectF()
        var first = true

        for (shape in allShapes) {
            val shapeBounds = shapeBoundsCache[shape] ?: updateShapeBounds(shape)
            
            if (shapeBounds.intersectsViewport(viewport)) {
                visibleCount++
                
                val bounds = shapeBounds.getBounds()
                if (first) {
                    totalBounds.set(bounds)
                    first = false
                } else {
                    totalBounds.union(bounds)
                }
            }
        }

        return VisibilityStats(
            totalShapes = allShapes.size,
            visibleShapes = visibleCount,
            visibleBounds = if (first) RectF() else totalBounds
        )
    }

    /**
     * Statistics about visibility calculation
     */
    data class VisibilityStats(
        val totalShapes: Int,
        val visibleShapes: Int,
        val visibleBounds: RectF
    )
}