package com.wyldsoft.notes.utils

import android.graphics.RectF
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape

/**
 * Utility class for eraser functionality including partial refresh optimization
 * Handles detection of shapes to erase and calculation of affected screen areas
 */
object EraserUtils {
    private const val TAG = "EraserUtils"

    // Default eraser radius in pixels
    private const val DEFAULT_ERASER_RADIUS = 20f

    /**
     * Data class to hold erasing session information including affected screen area
     */
    data class ErasingSession(
        val erasedShapes: MutableSet<DrawingShape> = mutableSetOf(),
        val affectedBounds: RectF = RectF()
    ) {
        /**
         * Add a shape to the erasing session and update affected bounds
         */
        fun addErasedShape(shape: DrawingShape) {
            erasedShapes.add(shape)

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
         * Get the affected bounds with additional padding for eraser width
         */
        fun getRefreshBounds(eraserRadius: Float = DEFAULT_ERASER_RADIUS): RectF {
            val paddedBounds = RectF(affectedBounds)
            paddedBounds.inset(-eraserRadius * 2, -eraserRadius * 2)
            return paddedBounds
        }

        /**
         * Check if this session has any erased shapes
         */
        fun hasErasedShapes(): Boolean = erasedShapes.isNotEmpty()

        /**
         * Clear the session data
         */
        fun clear() {
            erasedShapes.clear()
            affectedBounds.setEmpty()
        }
    }

    /**
     * Find shapes that should be erased by a single touch point
     * @param point The eraser touch point
     * @param availableShapes List of shapes that can be erased
     * @param eraserRadius Radius of the eraser tool
     * @return List of shapes that intersect with the eraser point
     */
    fun findShapesToEraseAtPoint(
        point: TouchPoint,
        availableShapes: List<DrawingShape>,
        eraserRadius: Float = DEFAULT_ERASER_RADIUS
    ): List<DrawingShape> {
        val pointList = TouchPointList().apply { add(point) }
        return availableShapes.filter { shape ->
            shape.hitTestPoints(pointList, eraserRadius)
        }
    }

    /**
     * Find shapes that should be erased by an eraser path (multiple touch points)
     * @param eraserPath TouchPointList representing the eraser movement
     * @param availableShapes List of shapes that can be erased
     * @param eraserRadius Radius of the eraser tool
     * @return List of shapes that intersect with the eraser path
     */
    fun findShapesToErase(
        eraserPath: TouchPointList,
        availableShapes: List<DrawingShape>,
        eraserRadius: Float = DEFAULT_ERASER_RADIUS
    ): List<DrawingShape> {
        return availableShapes.filter { shape ->
            shape.hitTestPoints(eraserPath, eraserRadius)
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
     * Calculate the bounding rectangle for touch points with eraser radius
     * @param touchPoints List of touch points from eraser movement
     * @param eraserRadius Radius of the eraser tool
     * @return RectF containing all touch points with padding
     */
    fun calculateTouchPointsBounds(
        touchPoints: List<TouchPoint>,
        eraserRadius: Float = DEFAULT_ERASER_RADIUS
    ): RectF {
        if (touchPoints.isEmpty()) return RectF()

        val bounds = RectF()
        touchPoints.forEach { point ->
            if (bounds.isEmpty) {
                bounds.set(point.x - eraserRadius, point.y - eraserRadius,
                    point.x + eraserRadius, point.y + eraserRadius)
            } else {
                bounds.union(point.x - eraserRadius, point.y - eraserRadius,
                    point.x + eraserRadius, point.y + eraserRadius)
            }
        }

        return bounds
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