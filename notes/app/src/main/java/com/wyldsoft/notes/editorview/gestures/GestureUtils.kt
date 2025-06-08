package com.wyldsoft.notes.editorview.gestures

import android.content.Context
import android.util.TypedValue
import androidx.compose.ui.unit.Dp

/**
 * Utility functions for gesture detection
 */
object GestureUtils {
    /**
     * Convert dp to pixels for the current context
     */
    fun convertDpToPixel(dp: Dp, context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.value,
            context.resources.displayMetrics
        )
    }
}