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

        return DatabaseShape(
            id = NanoIdUtils.randomNanoId(),
            noteId = noteId,
            touchPointList = drawingShape.touchPointList,
            shapeType = drawingShape.shapeType,
            texture = drawingShape.texture,
            strokeColor = drawingShape.strokeColor,
            strokeWidth = drawingShape.strokeWidth,
            isTransparent = drawingShape.isTransparent,
            penProfileData = storedPenProfile
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
}