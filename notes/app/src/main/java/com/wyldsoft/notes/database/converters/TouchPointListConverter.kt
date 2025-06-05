package com.wyldsoft.notes.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.data.TouchPointList

class TouchPointListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromTouchPointList(touchPointList: TouchPointList?): String? {
        if (touchPointList == null) return null

        // Convert TouchPointList to a serializable format
        val points = touchPointList.points.map { point ->
            SerializableTouchPoint(
                x = point.x,
                y = point.y,
                pressure = point.pressure,
                timestamp = point.timestamp,
                size = point.size
            )
        }

        return gson.toJson(points)
    }

    @TypeConverter
    fun toTouchPointList(data: String?): TouchPointList? {
        if (data == null) return null

        val type = object : TypeToken<List<SerializableTouchPoint>>() {}.type
        val points: List<SerializableTouchPoint> = gson.fromJson(data, type)

        val touchPointList = TouchPointList()
        points.forEach { serialPoint ->
            val touchPoint = TouchPoint().apply {
                x = serialPoint.x
                y = serialPoint.y
                pressure = serialPoint.pressure
                timestamp = serialPoint.timestamp
                size = serialPoint.size
            }
            touchPointList.add(touchPoint)
        }

        return touchPointList
    }
}

// Serializable version of TouchPoint for JSON conversion
data class SerializableTouchPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val timestamp: Long,
    val size: Float
)
