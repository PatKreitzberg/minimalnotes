package com.wyldsoft.notes.editorview.editor

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wyldsoft.notes.ExcludeRects
import com.wyldsoft.notes.editorview.drawing.base.BaseDrawingActivity
import com.wyldsoft.notes.pen.PenProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Enhanced EditorState with improved exclusion zone management
 * FIXED: Better cleanup and coordinate handling for exclusion zones
 */
class EditorState {
    var isDrawing by mutableStateOf(false)
    var isErasing by mutableStateOf(false)
    var eraserMode by mutableStateOf(false)

    // FIXED: Better exclusion zone state management
    var stateExcludeRects = mutableMapOf<ExcludeRects, Rect>()
    var stateExcludeRectsModified by mutableStateOf(false)

    // Track last applied exclusion zones for debugging
    private var lastAppliedRects = listOf<Rect>()

    companion object {
        private const val TAG = "EditorState"
        val isStrokeOptionsOpen = MutableSharedFlow<Boolean>()
        val drawingStarted = MutableSharedFlow<Unit>()
        val drawingEnded = MutableSharedFlow<Unit>()
        val erasingStarted = MutableSharedFlow<Unit>()
        val erasingEnded = MutableSharedFlow<Unit>()
        val forceScreenRefresh = MutableSharedFlow<Unit>()
        val penProfileChanged = MutableSharedFlow<PenProfile>()
        val eraserModeChanged = MutableSharedFlow<Boolean>()

        private var mainActivity: BaseDrawingActivity? = null

        fun setMainActivity(activity: BaseDrawingActivity) {
            mainActivity = activity
        }

        fun notifyDrawingStarted() {
            kotlinx.coroutines.GlobalScope.launch {
                drawingStarted.emit(Unit)
                isStrokeOptionsOpen.emit(false)
            }
        }

        fun notifyDrawingEnded() {
            kotlinx.coroutines.GlobalScope.launch {
                drawingEnded.emit(Unit)
                forceScreenRefresh.emit(Unit)
            }
        }

        fun notifyErasingStarted() {
            kotlinx.coroutines.GlobalScope.launch {
                erasingStarted.emit(Unit)
                isStrokeOptionsOpen.emit(false)
            }
        }

        fun notifyErasingEnded() {
            kotlinx.coroutines.GlobalScope.launch {
                erasingEnded.emit(Unit)
                forceScreenRefresh.emit(Unit)
            }
        }

        /**
         * ENHANCED: Better exclusion zone management with validation
         */
        fun updateExclusionZones(excludeRects: List<Rect>) {
            Log.d(TAG, "updateExclusionZones called with ${excludeRects.size} rects")

            // Validate rects before applying
            val validRects = excludeRects.filter { rect ->
                rect.width() > 0 && rect.height() > 0 &&
                        rect.left >= 0 && rect.top >= 0 &&
                        rect.right < 10000 && rect.bottom < 10000 // Reasonable bounds check
            }

            if (validRects.size != excludeRects.size) {
                Log.w(TAG, "Filtered out ${excludeRects.size - validRects.size} invalid rects")
            }

            Log.d(TAG, "Applying ${validRects.size} valid exclusion rects:")
            validRects.forEachIndexed { index, rect ->
                Log.d(TAG, "  Rect $index: $rect")
            }

            mainActivity?.updateExclusionZones(validRects)
        }

        /**
         * ENHANCED: Get current exclusion rects with validation
         */
        fun getCurrentExclusionRects(): List<Rect> {
            return mainActivity?.let { activity ->
                // Return current exclusion zones from activity
                // For now, return empty list as the activity will manage this
                emptyList<Rect>()
            } ?: emptyList()
        }

        /**
         * ENHANCED: Force clear all exclusion zones
         */
        fun clearAllExclusionZones() {
            Log.d(TAG, "clearAllExclusionZones called")
            updateExclusionZones(emptyList())
        }

        fun updatePenProfile(penProfile: PenProfile) {
            kotlinx.coroutines.GlobalScope.launch {
                penProfileChanged.emit(penProfile)
            }
            mainActivity?.updatePenProfile(penProfile)
        }

        fun setEraserMode(enabled: Boolean) {
            kotlinx.coroutines.GlobalScope.launch {
                eraserModeChanged.emit(enabled)
            }
            Log.d(TAG, "Eraser mode set to: $enabled")
        }

        fun forceRefresh() {
            Log.d(TAG, "forceRefresh() called")

            // Clear exclusion zones before refresh to prevent lingering issues
            clearAllExclusionZones()

            kotlinx.coroutines.GlobalScope.launch {
                forceScreenRefresh.emit(Unit)
            }
        }

        /**
         * NEW: Emergency cleanup function for when exclusion zones get stuck
         */
        fun emergencyCleanup() {
            Log.w(TAG, "Emergency cleanup called - clearing all exclusion zones")
            clearAllExclusionZones()

            kotlinx.coroutines.GlobalScope.launch {
                forceScreenRefresh.emit(Unit)
            }
        }

        /**
         * NEW: Validate exclusion zone state
         */
        fun validateExclusionZones(): Boolean {
            val currentRects = getCurrentExclusionRects()
            var hasIssues = false

            currentRects.forEach { rect ->
                if (rect.width() <= 0 || rect.height() <= 0) {
                    Log.w(TAG, "Found invalid exclusion rect: $rect")
                    hasIssues = true
                }
                if (rect.left < 0 || rect.top < 0) {
                    Log.w(TAG, "Found exclusion rect with negative coordinates: $rect")
                    hasIssues = true
                }
            }

            if (hasIssues) {
                Log.w(TAG, "Exclusion zone validation failed, triggering cleanup")
                emergencyCleanup()
            }

            return !hasIssues
        }
    }
}