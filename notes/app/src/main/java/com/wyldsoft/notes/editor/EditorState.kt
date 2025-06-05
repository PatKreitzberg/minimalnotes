package com.wyldsoft.notes.editor

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wyldsoft.notes.ExcludeRects
import com.wyldsoft.notes.base.BaseDrawingActivity
import com.wyldsoft.notes.pen.PenProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Manages the state of the drawing editor including drawing and erasing modes
 */
class EditorState {
    var isDrawing by mutableStateOf(false)
    var isErasing by mutableStateOf(false)
    var eraserMode by mutableStateOf(false) // For toolbar eraser button
    var stateExcludeRects = mutableMapOf<ExcludeRects, Rect>()
    var stateExcludeRectsModified by mutableStateOf(false)

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

        fun updateExclusionZones(excludeRects: List<Rect>) {
            mainActivity?.updateExclusionZones(excludeRects)
        }

        fun getCurrentExclusionRects(): List<Rect> {
            return mainActivity?.let { activity ->
                emptyList<Rect>()
            } ?: emptyList()
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
            Log.d(TAG, "forceRefresh()")
            kotlinx.coroutines.GlobalScope.launch {
                forceScreenRefresh.emit(Unit)
            }
        }
    }
}