package com.wyldsoft.notes.backend.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.wyldsoft.notes.backend.database.converters.TouchPointListConverter
import com.wyldsoft.notes.backend.database.converters.PenProfileConverter
import com.onyx.android.sdk.pen.data.TouchPointList

@Entity(
    tableName = "shapes",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
@TypeConverters(TouchPointListConverter::class, PenProfileConverter::class)
data class Shape(
    @PrimaryKey
    val id: String,
    val noteId: String,

    // Raw touch data for HTR
    val touchPointList: TouchPointList,

    // Processed shape data
    val shapeType: Int,
    val texture: Int = 0,
    val strokeColor: Int,
    val strokeWidth: Float,
    val isTransparent: Boolean = false,

    // Pen profile data used when creating this shape
    val penProfileData: StoredPenProfile,

    // Bounding box for efficient visibility calculations
    val boundingMinX: Float,
    val boundingMinY: Float,
    val boundingMaxX: Float,
    val boundingMaxY: Float,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis()
)

// Data class to store pen profile information
data class StoredPenProfile(
    val strokeWidth: Float,
    val penType: String, // Store as string to avoid enum dependency issues
    val strokeColor: Int,
    val profileId: Int
)