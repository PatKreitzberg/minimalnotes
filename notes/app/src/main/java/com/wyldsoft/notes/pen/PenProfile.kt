package com.wyldsoft.notes.pen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.wyldsoft.notes.base.SDKType

data class PenProfile(
    val strokeWidth: Float,
    val penType: PenType,
    val strokeColor: Color
) {
    companion object {
        fun getDefaultProfile(penType: PenType): PenProfile {
            val defaultStrokeWidth = when (penType) {
                PenType.BALLPEN -> 5f
                PenType.FOUNTAIN -> 8f
                PenType.MARKER -> 20f
                PenType.PENCIL -> 3f
                PenType.CHARCOAL -> 15f
                PenType.CHARCOAL_V2 -> 15f
                PenType.NEO_BRUSH -> 25f
                PenType.DASH -> 6f
            }

            return PenProfile(
                strokeWidth = defaultStrokeWidth,
                penType = penType,
                strokeColor = Color.Black
            )
        }
    }

    fun getColorAsInt(): Int = strokeColor.toArgb()

    // Legacy method for backward compatibility
    @Deprecated("Use getStrokeStyleForSDK instead", ReplaceWith("getStrokeStyleForSDK(SDKType.ONYX)"))
    fun getOnyxStrokeStyle(): Int {
        return getStrokeStyleForSDK(SDKType.ONYX)
    }

    // New SDK-agnostic method
    fun getStrokeStyleForSDK(sdkType: SDKType): Int {
        return when (sdkType) {
            SDKType.ONYX -> getOnyxStrokeStyleInternal()
            SDKType.HUION -> getHuionStrokeStyle()
            SDKType.WACOM -> getWacomStrokeStyle()
            SDKType.GENERIC -> 0 // Default style
        }
    }

    private fun getOnyxStrokeStyleInternal(): Int {
        // Onyx stroke style mapping based on pen type
        return when (penType) {
            PenType.BALLPEN -> 0
            PenType.FOUNTAIN -> 1
            PenType.MARKER -> 2
            PenType.PENCIL -> 3
            PenType.CHARCOAL -> 4
            PenType.CHARCOAL_V2 -> 5
            PenType.NEO_BRUSH -> 6
            PenType.DASH -> 7
        }
    }

    private fun getHuionStrokeStyle(): Int {
        // Future: Implement Huion-specific stroke style mapping
        return when (penType) {
            PenType.BALLPEN -> 0
            PenType.FOUNTAIN -> 1
            PenType.MARKER -> 2
            PenType.PENCIL -> 3
            PenType.CHARCOAL -> 4
            PenType.CHARCOAL_V2 -> 4  // Map to same as charcoal
            PenType.NEO_BRUSH -> 5
            PenType.DASH -> 0  // Map to ballpen for Huion
        }
    }

    private fun getWacomStrokeStyle(): Int {
        // Future: Implement Wacom-specific stroke style mapping
        return when (penType) {
            PenType.BALLPEN -> 1
            PenType.FOUNTAIN -> 2
            PenType.MARKER -> 3
            PenType.PENCIL -> 0
            PenType.CHARCOAL -> 4
            PenType.CHARCOAL_V2 -> 4
            PenType.NEO_BRUSH -> 5
            PenType.DASH -> 1
        }
    }
}