package com.wyldsoft.notes

import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

enum class ExcludeRects {
    StrokeOptions
}

class EditorState {
    var isDrawing by mutableStateOf(false)
    var stateExcludeRects = mutableMapOf<ExcludeRects, Rect>()
    var stateExcludeRectsModified by mutableStateOf(false)

    companion object {
        val refreshUi = MutableSharedFlow<Unit>()
        val isStrokeOptionsOpen = MutableSharedFlow<Boolean>()
        val drawingStarted = MutableSharedFlow<Unit>()
        val drawingEnded = MutableSharedFlow<Unit>()
        val forceScreenRefresh = MutableSharedFlow<Unit>()
        val penProfileChanged = MutableSharedFlow<PenProfile>()

        private var mainActivity: MainActivity? = null

        fun setMainActivity(activity: MainActivity) {
            mainActivity = activity
        }

        fun notifyDrawingStarted() {
            kotlinx.coroutines.GlobalScope.launch {
                drawingStarted.emit(Unit)
                isStrokeOptionsOpen.emit(false) // Close panel when drawing starts
            }
        }

        fun notifyDrawingEnded() {
            kotlinx.coroutines.GlobalScope.launch {
                drawingEnded.emit(Unit)
                forceScreenRefresh.emit(Unit) // Force refresh when drawing ends
            }
        }

        fun updateExclusionZones(excludeRects: List<Rect>) {
            mainActivity?.updateExclusionZones(excludeRects)
        }

        fun getCurrentExclusionRects(): List<Rect> {
            return mainActivity?.let { activity ->
                // Get current exclusion rects from the activity's editor state
                // This is a simplified approach - in practice you might want to maintain this centrally
                emptyList<Rect>()
            } ?: emptyList()
        }

        fun updatePenProfile(penProfile: PenProfile) {
            kotlinx.coroutines.GlobalScope.launch {
                penProfileChanged.emit(penProfile)
            }
            mainActivity?.updatePenProfile(penProfile)
        }

        fun forceRefresh() {
            kotlinx.coroutines.GlobalScope.launch {
                forceScreenRefresh.emit(Unit)
            }
            mainActivity?.let { activity ->
                activity.runOnUiThread {
                    // Force UI refresh on main thread
                    kotlinx.coroutines.GlobalScope.launch {
                        refreshUi.emit(Unit)
                    }
                }
            }
        }
    }
}