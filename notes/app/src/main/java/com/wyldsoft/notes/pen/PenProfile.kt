package com.wyldsoft.notes.pen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import com.wyldsoft.notes.base.SDKType

data class PenProfile(
    val strokeWidth: Float,
    var penType: PenType, // Made mutable to allow pen type switching
    val strokeColor: Color,
    val profileId: Int = 0 // Added profile ID for identification
) {
    companion object {
        fun getDefaultProfile(penType: PenType, profileId: Int = 0): PenProfile {
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
                strokeColor = Color.Black,
                profileId = profileId
            )
        }

        // Create 5 default profiles with different pen types and colors
        fun createDefaultProfiles(): List<PenProfile> {
            return listOf(
                PenProfile(5f, PenType.BALLPEN, Color.Black, 0),
                PenProfile(8f, PenType.FOUNTAIN, Color.Blue, 1),
                PenProfile(20f, PenType.MARKER, Color.Red, 2),
                PenProfile(3f, PenType.PENCIL, Color.Gray, 3),
                PenProfile(15f, PenType.CHARCOAL, Color(0xFF8B4513), 4) // Brown
            )
        }
    }

    fun getColorAsInt(): Int = strokeColor.toArgb()

    // Create a copy with different properties
    fun withPenType(newPenType: PenType): PenProfile = copy(penType = newPenType)
    fun withColor(newColor: Color): PenProfile = copy(strokeColor = newColor)
    fun withStrokeWidth(newWidth: Float): PenProfile = copy(strokeWidth = newWidth)

    // Legacy method for backward compatibility
    @Deprecated("Use getStrokeStyleForSDK instead", ReplaceWith("getStrokeStyleForSDK(SDKType.ONYX)"))
    fun getOnyxStrokeStyle(): Int = getStrokeStyleForSDK(SDKType.ONYX)

    // SDK-agnostic method
    fun getStrokeStyleForSDK(sdkType: SDKType): Int {
        return when (sdkType) {
            SDKType.ONYX -> getOnyxStrokeStyleInternal()
            SDKType.HUION -> getHuionStrokeStyle()
            SDKType.WACOM -> getWacomStrokeStyle()
            SDKType.GENERIC -> 0
        }
    }

    private fun getOnyxStrokeStyleInternal(): Int {
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
        return when (penType) {
            PenType.BALLPEN -> 0
            PenType.FOUNTAIN -> 1
            PenType.MARKER -> 2
            PenType.PENCIL -> 3
            PenType.CHARCOAL -> 4
            PenType.CHARCOAL_V2 -> 4
            PenType.NEO_BRUSH -> 5
            PenType.DASH -> 0
        }
    }

    private fun getWacomStrokeStyle(): Int {
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