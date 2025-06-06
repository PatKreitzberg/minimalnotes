package com.wyldsoft.notes.utils

import android.graphics.RectF
import com.wyldsoft.notes.editorview.drawing.shape.Shape
import com.onyx.android.sdk.data.note.TouchPoint

/**
 * Utility class for enhanced shape boundary calculations
 * Provides methods to accurately calculate bounding rectangles for shapes
 * to support optimized partial refresh operations
 */
object ShapeBoundsUtils {
    private const val TAG = "ShapeBoundsUtils"

    /**
     * Calculate accurate bounding rectangle for a shape including stroke width
     * @param shape The shape to calculate bounds for
     * @return RectF containing the shape bounds including stroke padding
     */
    fun calculateShapeBoundsWithStroke(shape: Shape): RectF {
        // Ensure the shape has updated its bounding rectangle
        shape.updateShapeRect()

        val bounds = shape.boundingRect ?: run {
            // Fallback: calculate bounds from touch points if shape bounds are null
            calculateBoundsFromTouchPoints(shape)
        }

        if (bounds.isEmpty) {
            return RectF()
        }

        // Add padding for stroke width
        val strokePadding = shape.strokeWidth / 2f
        val paddedBounds = RectF(bounds)
        paddedBounds.inset(-strokePadding, -strokePadding)

        return paddedBounds
    }

    /**
     * Calculate bounding rectangle directly from touch points
     * Useful when shape.boundingRect is null or inaccurate
     */
    private fun calculateBoundsFromTouchPoints(shape: Shape): RectF {
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
     * Calculate combined bounding rectangle for multiple shapes
     * @param shapes Collection of shapes to combine
     * @return RectF containing all shapes with stroke padding
     */
    fun calculateCombinedBounds(shapes: Collection<Shape>): RectF {
        if (shapes.isEmpty()) {
            return RectF()
        }

        val combinedBounds = RectF()
        var first = true

        for (shape in shapes) {
            val shapeBounds = calculateShapeBoundsWithStroke(shape)
            if (!shapeBounds.isEmpty) {
                if (first) {
                    combinedBounds.set(shapeBounds)
                    first = false
                } else {
                    combinedBounds.union(shapeBounds)
                }
            }
        }

        return combinedBounds
    }

    /**
     * Calculate bounds for a list of touch points with radius padding
     * @param touchPoints List of touch points
     * @param radius Padding radius around each point
     * @return RectF containing all points with radius padding
     */
    fun calculateTouchPointsBounds(touchPoints: List<TouchPoint>, radius: Float): RectF {
        if (touchPoints.isEmpty()) {
            return RectF()
        }

        var minX = touchPoints[0].x - radius
        var maxX = touchPoints[0].x + radius
        var minY = touchPoints[0].y - radius
        var maxY = touchPoints[0].y + radius

        for (point in touchPoints) {
            val pointMinX = point.x - radius
            val pointMaxX = point.x + radius
            val pointMinY = point.y - radius
            val pointMaxY = point.y + radius

            minX = minX.coerceAtMost(pointMinX)
            maxX = maxX.coerceAtLeast(pointMaxX)
            minY = minY.coerceAtMost(pointMinY)
            maxY = maxY.coerceAtLeast(pointMaxY)
        }

        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * Expand bounds by a specified amount
     * @param bounds Original bounds to expand
     * @param expansion Amount to expand in all directions
     * @return New RectF with expanded bounds
     */
    fun expandBounds(bounds: RectF, expansion: Float): RectF {
        val expandedBounds = RectF(bounds)
        expandedBounds.inset(-expansion, -expansion)
        return expandedBounds
    }

    /**
     * Constrain bounds to fit within screen dimensions
     * @param bounds Bounds to constrain
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @return Constrained bounds that fit within screen
     */
    fun constrainToScreen(bounds: RectF, screenWidth: Int, screenHeight: Int): RectF {
        val constrainedBounds = RectF(bounds)

        constrainedBounds.left = constrainedBounds.left.coerceAtLeast(0f)
        constrainedBounds.top = constrainedBounds.top.coerceAtLeast(0f)
        constrainedBounds.right = constrainedBounds.right.coerceAtMost(screenWidth.toFloat())
        constrainedBounds.bottom = constrainedBounds.bottom.coerceAtMost(screenHeight.toFloat())

        return constrainedBounds
    }

    /**
     * Check if bounds are large enough to warrant partial refresh
     * @param bounds Bounds to check
     * @param minArea Minimum area threshold
     * @param maxAreaRatio Maximum area ratio relative to screen
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     * @return True if partial refresh is recommended
     */
    fun shouldUsePartialRefresh(
        bounds: RectF,
        minArea: Float = 100f,
        maxAreaRatio: Float = 0.5f,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        if (bounds.isEmpty) {
            return false
        }

        val boundsArea = bounds.width() * bounds.height()
        val screenArea = screenWidth.toFloat() * screenHeight.toFloat()

        return boundsArea >= minArea && boundsArea <= screenArea * maxAreaRatio
    }

    /**
     * Merge two bounding rectangles
     * @param bounds1 First bounds
     * @param bounds2 Second bounds
     * @return Merged bounds containing both rectangles
     */
    fun mergeBounds(bounds1: RectF, bounds2: RectF): RectF {
        if (bounds1.isEmpty) {
            return RectF(bounds2)
        }
        if (bounds2.isEmpty) {
            return RectF(bounds1)
        }

        val merged = RectF(bounds1)
        merged.union(bounds2)
        return merged
    }

    /**
     * Check if two bounds rectangles intersect
     * @param bounds1 First bounds
     * @param bounds2 Second bounds
     * @return True if bounds intersect
     */
    fun boundsIntersect(bounds1: RectF, bounds2: RectF): Boolean {
        return RectF.intersects(bounds1, bounds2)
    }

    /**
     * Calculate the intersection of two bounds rectangles
     * @param bounds1 First bounds
     * @param bounds2 Second bounds
     * @return Intersection bounds, or empty rect if no intersection
     */
    fun getIntersection(bounds1: RectF, bounds2: RectF): RectF {
        val intersection = RectF()
        return if (intersection.setIntersect(bounds1, bounds2)) {
            intersection
        } else {
            RectF() // Empty rectangle
        }
    }
}