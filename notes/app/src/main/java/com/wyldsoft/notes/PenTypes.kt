package com.wyldsoft.notes


import androidx.compose.ui.graphics.Color

enum class PenType(val displayName: String) {
    BALLPEN("Ball Pen"),
    FOUNTAIN("Fountain Pen"),
    MARKER("Marker")
}

data class PenSetting(
    val strokeSize: Float,
    val color: Color
)

