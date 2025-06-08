package com.wyldsoft.notes.utils

import android.graphics.RectF
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape

/**
 * Utility class for refresh functionality including partial refresh optimization
 * Handles calculation of affected screen areas and bounds management for various operations
 */
object RefreshUtils {
    private const val TAG = "RefreshUtils"

    // Default tool radius in pixels for bounds calculations
    private const val DEFAULT_TOOL_RADIUS = 20f

    /**
     * Data class to hold refresh session information including affected screen area
     * Can be used for erasing, drawing, or any operation that affects screen regions
     */
    data class RefreshSession(
        val affectedShapes: MutableSet<DrawingShape> = mutableSetOf(),
        val affectedBounds: RectF = RectF()
    ) {
        /**
         * Add a shape to the refresh session and update affected bounds
         */
        fun addAffectedShape(shape: DrawingShape) {
            affectedShapes.add(shape)

            // Update bounding rectangle of the shape if it exists
            shape.updateShapeRect()
            shape.boundingRect?.let { shapeBounds ->
                if (affectedBounds.isEmpty) {
                    affectedBounds.set(shapeBounds)
                } else {
                    affectedBounds.union(shapeBounds)
                }
            }
        }

        /**
         * Get the affected bounds with additional padding for tool width
         */
        fun getRefreshBounds(toolRadius: Float = DEFAULT_TOOL_RADIUS): RectF {
            val paddedBounds = RectF(affectedBounds)
            paddedBounds.inset(-toolRadius * 2, -toolRadius * 2)
            return paddedBounds
        }

        /**
         * Check if this session has any affected shapes
         */
        fun hasAffectedShapes(): Boolean = affectedShapes.isNotEmpty()

        /**
         * Clear the session data
         */
        fun clear() {
            affectedShapes.clear()
            affectedBounds.setEmpty()
        }
    }

    /**
     * Find shapes that intersect with a single touch point
     * @param point The touch point to test
     * @param availableShapes List of shapes to test against
     * @param toolRadius Radius of the tool
     * @return List of shapes that intersect with the point
     */
    fun findShapesAtPoint(
        point: TouchPoint,
        availableShapes: List<DrawingShape>,
        toolRadius: Float = DEFAULT_TOOL_RADIUS
    ): List<DrawingShape> {
        val pointList = TouchPointList().apply { add(point) }
        return availableShapes.filter { shape ->
            shape.hitTestPoints(pointList, toolRadius)
        }
    }

    /**
     * Find shapes that intersect with a path of touch points
     * @param touchPath TouchPointList representing the tool movement
     * @param availableShapes List of shapes to test against
     * @param toolRadius Radius of the tool
     * @return List of shapes that intersect with the path
     */
    fun findShapesInPath(
        touchPath: TouchPointList,
        availableShapes: List<DrawingShape>,
        toolRadius: Float = DEFAULT_TOOL_RADIUS
    ): List<DrawingShape> {
        return availableShapes.filter { shape ->
            shape.hitTestPoints(touchPath, toolRadius)
        }
    }

    /**
     * Calculate the bounding rectangle for a collection of shapes
     * @param shapes Collection of shapes to calculate bounds for
     * @return RectF containing all shapes, or empty rect if no shapes
     */
    fun calculateShapesBounds(shapes: Collection<DrawingShape>): RectF {
        val bounds = RectF()

        shapes.forEach { shape ->
            shape.updateShapeRect()
            shape.boundingRect?.let { shapeBounds ->
                if (bounds.isEmpty) {
                    bounds.set(shapeBounds)
                } else {
                    bounds.union(shapeBounds)
                }
            }
        }

        return bounds
    }

    /**
     * Calculate the bounding rectangle for touch points with tool radius
     * @param touchPoints List of touch points from tool movement
     * @param toolRadius Radius of the tool
     * @return RectF containing all touch points with padding
     */
    fun calculateTouchPointsBounds(
        touchPoints: List<TouchPoint>,
        toolRadius: Float = DEFAULT_TOOL_RADIUS
    ): RectF {
        if (touchPoints.isEmpty()) return RectF()

        val bounds = RectF()
        touchPoints.forEach { point ->
            if (bounds.isEmpty) {
                bounds.set(point.x - toolRadius, point.y - toolRadius,
                    point.x + toolRadius, point.y + toolRadius)
            } else {
                bounds.union(point.x - toolRadius, point.y - toolRadius,
                    point.x + toolRadius, point.y + toolRadius)
            }
        }

        return bounds
    }

    /**
     * Calculate combined bounds from multiple sources
     * @param boundsSet List of RectF bounds to combine
     * @return Combined RectF containing all input bounds
     */
    fun calculateCombinedBounds(boundsSet: List<RectF>): RectF {
        val combinedBounds = RectF()
        
        boundsSet.forEach { bounds ->
            if (!bounds.isEmpty) {
                if (combinedBounds.isEmpty) {
                    combinedBounds.set(bounds)
                } else {
                    combinedBounds.union(bounds)
                }
            }
        }
        
        return combinedBounds
    }

    /**
     * Validate and constrain refresh bounds to screen dimensions
     * @param bounds The calculated bounds to validate
     * @param screenWidth Width of the screen/surface
     * @param screenHeight Height of the screen/surface
     * @return Validated RectF constrained to screen bounds
     */
    fun validateRefreshBounds(
        bounds: RectF,
        screenWidth: Int,
        screenHeight: Int
    ): RectF {
        val validatedBounds = RectF(bounds)

        // Ensure bounds are within screen limits
        validatedBounds.left = validatedBounds.left.coerceAtLeast(0f)
        validatedBounds.top = validatedBounds.top.coerceAtLeast(0f)
        validatedBounds.right = validatedBounds.right.coerceAtMost(screenWidth.toFloat())
        validatedBounds.bottom = validatedBounds.bottom.coerceAtMost(screenHeight.toFloat())

        // Ensure minimum refresh area (avoid too small partial refreshes)
        val minRefreshSize = 50f
        if (validatedBounds.width() < minRefreshSize) {
            val center = validatedBounds.centerX()
            validatedBounds.left = (center - minRefreshSize / 2).coerceAtLeast(0f)
            validatedBounds.right = (center + minRefreshSize / 2).coerceAtMost(screenWidth.toFloat())
        }

        if (validatedBounds.height() < minRefreshSize) {
            val center = validatedBounds.centerY()
            validatedBounds.top = (center - minRefreshSize / 2).coerceAtLeast(0f)
            validatedBounds.bottom = (center + minRefreshSize / 2).coerceAtMost(screenHeight.toFloat())
        }

        return validatedBounds
    }
}