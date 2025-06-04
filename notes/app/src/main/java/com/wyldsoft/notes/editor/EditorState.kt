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

        fun forceRefresh() {
            Log.d("EditorState:", "forceRefresh()");
            kotlinx.coroutines.GlobalScope.launch {
                forceScreenRefresh.emit(Unit)
            }
            mainActivity?.let { activity ->
                activity.runOnUiThread {
                    kotlinx.coroutines.GlobalScope.launch {
                        refreshUi.emit(Unit)
                    }
                }
            }
        }
    }
}