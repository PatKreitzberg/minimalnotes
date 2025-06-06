package com.wyldsoft.notes.editorview.drawing.onyx

import android.util.Log
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList
import com.wyldsoft.notes.editorview.drawing.shape.Shape
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.pen.PenType
import com.wyldsoft.notes.data.ShapeFactory

/**
 * Manages shape creation, storage, and manipulation for the Onyx drawing system
 * Handles the creation of different shape types based on pen profiles
 * and maintains the collection of drawn shapes
 */
class OnyxShapeManager(
    private val renderingManager: OnyxRenderingManager
) {
    companion object {
        private const val TAG = "OnyxShapeManager"
    }

    // Store all drawn shapes for re-rendering and erasing
    private val drawnShapes = mutableListOf<Shape>()

    /**
     * Create a new shape from touch points and current pen profile
     * @param touchPointList The touch input data
     * @param penProfile Current pen configuration
     * @return Created shape ready for rendering
     */
    fun createShapeFromTouchPoints(
        touchPointList: TouchPointList,
        penProfile: PenProfile
    ): Shape {
        val shapeType = mapPenTypeToShapeType(penProfile.penType)
        val shape = ShapeFactory.createShape(shapeType)

        configureShape(shape, touchPointList, penProfile, shapeType)

        return shape
    }

    /**
     * Add a shape to the drawn shapes collection
     * @param shape The shape to add
     */
    fun addShape(shape: Shape) {
        drawnShapes.add(shape)
        Log.d(TAG, "Added shape, total shapes: ${drawnShapes.size}")
    }

    /**
     * Remove multiple shapes from the collection
     * @param shapesToRemove Collection of shapes to remove
     * @return Number of shapes actually removed
     */
    fun removeShapes(shapesToRemove: Collection<Shape>): Int {
        val initialSize = drawnShapes.size
        drawnShapes.removeAll(shapesToRemove.toSet())
        val removedCount = initialSize - drawnShapes.size

        Log.d(TAG, "Removed $removedCount shapes, remaining: ${drawnShapes.size}")
        return removedCount
    }

    /**
     * Get all currently drawn shapes
     * @return Read-only list of all shapes
     */
    fun getAllShapes(): List<Shape> = drawnShapes.toList()

    /**
     * Clear all drawn shapes
     */
    fun clearShapes() {
        val clearedCount = drawnShapes.size
        drawnShapes.clear()
        Log.d(TAG, "Cleared $clearedCount shapes")
    }

    /**
     * Replace all shapes with a new collection (used when loading from database)
     * @param newShapes The shapes to replace current collection with
     */
    fun replaceAllShapes(newShapes: List<Shape>) {
        drawnShapes.clear()
        drawnShapes.addAll(newShapes)
        Log.d(TAG, "Replaced all shapes with ${newShapes.size} shapes")
    }

    /**
     * Get the number of currently drawn shapes
     * @return Count of shapes
     */
    fun getShapeCount(): Int = drawnShapes.size

    /**
     * Check if there are any drawn shapes
     * @return True if shapes collection is not empty
     */
    fun hasShapes(): Boolean = drawnShapes.isNotEmpty()

    /**
     * Render a specific shape to the current bitmap
     * @param shape The shape to render
     */
    fun renderShape(shape: Shape) {
        renderingManager.renderShapeToBitmap(shape)
    }

    /**
     * Recreate the entire drawing from all stored shapes
     * This is used after erasing operations or when loading from database
     */
    fun recreateDrawingFromShapes() {
        renderingManager.recreateBitmapFromShapes(drawnShapes)
        Log.d(TAG, "Recreated drawing from ${drawnShapes.size} shapes")
    }

    /**
     * Map pen type to corresponding shape type constant
     */
    private fun mapPenTypeToShapeType(penType: PenType): Int {
        return when (penType) {
            PenType.BALLPEN, PenType.PENCIL -> ShapeFactory.SHAPE_PENCIL_SCRIBBLE
            PenType.FOUNTAIN -> ShapeFactory.SHAPE_BRUSH_SCRIBBLE
            PenType.MARKER -> ShapeFactory.SHAPE_MARKER_SCRIBBLE
            PenType.CHARCOAL, PenType.CHARCOAL_V2 -> ShapeFactory.SHAPE_CHARCOAL_SCRIBBLE
            PenType.NEO_BRUSH -> ShapeFactory.SHAPE_NEO_BRUSH_SCRIBBLE
            PenType.DASH -> ShapeFactory.SHAPE_PENCIL_SCRIBBLE // Default to pencil for dash
        }
    }

    /**
     * Configure a shape with the provided parameters
     */
    private fun configureShape(
        shape: Shape,
        touchPointList: TouchPointList,
        penProfile: PenProfile,
        shapeType: Int
    ) {
        shape.setTouchPointList(touchPointList)
            .setStrokeColor(penProfile.getColorAsInt())
            .setStrokeWidth(penProfile.strokeWidth)
            .setShapeType(shapeType)

        // Set texture for charcoal variants
        when (penProfile.penType) {
            PenType.CHARCOAL_V2 -> {
                shape.setTexture(com.onyx.android.sdk.data.note.PenTexture.CHARCOAL_SHAPE_V2)
            }
            PenType.CHARCOAL -> {
                shape.setTexture(com.onyx.android.sdk.data.note.PenTexture.CHARCOAL_SHAPE_V1)
            }
            else -> {
                // No special texture needed for other pen types
            }
        }
    }

    /**
     * Get shapes that intersect with given touch points (for erasing)
     * @param touchPoints Touch points to test intersection with
     * @param radius Test radius around touch points
     * @return List of shapes that intersect
     */
    fun getShapesIntersectingWithPoints(
        touchPoints: TouchPointList,
        radius: Float = 20f
    ): List<Shape> {
        return drawnShapes.filter { shape ->
            shape.hitTestPoints(touchPoints, radius)
        }
    }

    /**
     * Get a snapshot of available shapes for erasing operations
     * This creates a copy to avoid concurrent modification during erasing
     * @return Snapshot list of current shapes
     */
    fun getShapesSnapshot(): List<Shape> = drawnShapes.toList()
}