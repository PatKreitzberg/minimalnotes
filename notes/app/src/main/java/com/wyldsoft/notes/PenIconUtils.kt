package com.wyldsoft.notes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.wyldsoft.notes.pen.PenType

object PenIconUtils {
    fun getIconForPenType(penType: PenType): ImageVector {
        return when (penType) {
            PenType.BALLPEN -> Icons.Default.Edit // Ballpoint pen
            PenType.FOUNTAIN -> Icons.Default.Create // Fountain pen
            PenType.MARKER -> Icons.Default.FormatColorFill // Marker/highlighter
            PenType.PENCIL -> Icons.Default.Draw // Pencil
            PenType.CHARCOAL -> Icons.Default.Brush // Charcoal
            PenType.CHARCOAL_V2 -> Icons.Default.ColorLens // Charcoal V2
            PenType.NEO_BRUSH -> Icons.Default.Palette // Neo brush
            PenType.DASH -> Icons.Default.Timeline // Dash pen
        }
    }

    fun getContentDescriptionForPenType(penType: PenType): String {
        return when (penType) {
            PenType.BALLPEN -> "Ballpoint Pen"
            PenType.FOUNTAIN -> "Fountain Pen"
            PenType.MARKER -> "Marker"
            PenType.PENCIL -> "Pencil"
            PenType.CHARCOAL -> "Charcoal"
            PenType.CHARCOAL_V2 -> "Charcoal V2"
            PenType.NEO_BRUSH -> "Neo Brush"
            PenType.DASH -> "Dash Pen"
        }
    }
}