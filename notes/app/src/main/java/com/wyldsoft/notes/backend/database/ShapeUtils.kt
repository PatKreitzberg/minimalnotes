package com.wyldsoft.notes.backend.database

import com.wyldsoft.notes.backend.database.entities.Shape as DatabaseShape
import com.wyldsoft.notes.backend.database.entities.StoredPenProfile
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.pen.PenType
import com.wyldsoft.notes.data.ShapeFactory
import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.onyx.android.sdk.data.note.TouchPoint
import android.graphics.RectF

/**
 * Utility class to convert between drawing shapes and database shapes
 */
object ShapeUtils {

    /**
     * Convert a drawing shape to a database shape for storage
     */
    fun convertToDatabase(
        drawingShape: DrawingShape,
        noteId: String,
        penProfile: PenProfile
    ): DatabaseShape {
        val storedPenProfile = StoredPenProfile(
            strokeWidth = penProfile.strokeWidth,
            penType = penProfile.penType.name,
            strokeColor = penProfile.strokeColor.toArgb(),
            profileId = penProfile.profileId
        )

        // Calculate bounding box with stroke padding
        drawingShape.updateShapeRect()
        val bounds = drawingShape.boundingRect
        val strokePadding = drawingShape.strokeWidth / 2f
        
        val (minX, minY, maxX, maxY) = if (bounds != null && !bounds.isEmpty) {
            arrayOf(
                bounds.left - strokePadding,
                bounds.top - strokePadding,
                bounds.right + strokePadding,
                bounds.bottom + strokePadding
            )
        } else {
            // Fallback to touch points if bounds are unavailable
            calculateBoundsFromTouchPoints(drawingShape)
        }

        return DatabaseShape(
            id = NanoIdUtils.randomNanoId(),
            noteId = noteId,
            touchPointList = drawingShape.touchPointList,
            shapeType = drawingShape.shapeType,
            texture = drawingShape.texture,
            strokeColor = drawingShape.strokeColor,
            strokeWidth = drawingShape.strokeWidth,
            isTransparent = drawingShape.isTransparent,
            penProfileData = storedPenProfile,
            boundingMinX = minX,
            boundingMinY = minY,
            boundingMaxX = maxX,
            boundingMaxY = maxY
        )
    }

    /**
     * Convert a database shape back to a drawing shape for rendering
     */
    fun convertToDrawing(databaseShape: DatabaseShape): DrawingShape {
        val drawingShape = ShapeFactory.createShape(databaseShape.shapeType)

        drawingShape.setTouchPointList(databaseShape.touchPointList)
            .setShapeType(databaseShape.shapeType)
            .setTexture(databaseShape.texture)
            .setStrokeColor(databaseShape.strokeColor)
            .setStrokeWidth(databaseShape.strokeWidth)

        drawingShape.setTransparent(databaseShape.isTransparent)

        return drawingShape
    }

    /**
     * Convert stored pen profile back to PenProfile for UI
     */
    fun convertToPenProfile(storedProfile: StoredPenProfile): PenProfile {
        val penType = try {
            PenType.valueOf(storedProfile.penType)
        } catch (e: IllegalArgumentException) {
            PenType.BALLPEN // fallback
        }

        return PenProfile(
            strokeWidth = storedProfile.strokeWidth,
            penType = penType,
            strokeColor = Color(storedProfile.strokeColor),
            profileId = storedProfile.profileId
        )
    }

    /**
     * Filter shapes that intersect with the given bounding rectangle
     * This is a shared utility function used by various components that need to
     * find shapes within a specific area (e.g., erasing, partial refresh)
     * @param shapes List of shapes to filter
     * @param bounds Bounding rectangle to test intersection with
     * @return List of shapes that intersect with the bounds
     */
    fun filterShapesInBounds(shapes: List<DrawingShape>, bounds: RectF): List<DrawingShape> {
        return shapes.filter { shape ->
            // Ensure shape rect is updated with fresh calculation
            shape.updateShapeRect()
            
            // Check if shape has valid bounding rect and intersects with bounds
            shape.boundingRect?.let { shapeBounds ->
                if (!shapeBounds.isEmpty) {
                    RectF.intersects(bounds, shapeBounds)
                } else {
                    false
                }
            } ?: false
        }
    }

    /**
     * Calculate bounding box from touch points when shape bounds are unavailable
     */
    private fun calculateBoundsFromTouchPoints(drawingShape: DrawingShape): Array<Float> {
        val touchPoints = drawingShape.touchPointList?.points
        
        if (touchPoints.isNullOrEmpty()) {
            return arrayOf(0f, 0f, 0f, 0f)
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

        // Add stroke padding
        val strokePadding = drawingShape.strokeWidth / 2f
        return arrayOf(
            minX - strokePadding,
            minY - strokePadding,
            maxX + strokePadding,
            maxY + strokePadding
        )
    }
}