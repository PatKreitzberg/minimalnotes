package com.wyldsoft.notes.onyx

import android.graphics.Rect
import com.onyx.android.sdk.pen.TouchHelper
import com.wyldsoft.notes.base.BaseTouchHelper

class OnyxTouchHelperWrapper(private val onyxTouchHelper: TouchHelper) : BaseTouchHelper() {

    override fun setRawDrawingEnabled(enabled: Boolean) {
        onyxTouchHelper.setRawDrawingEnabled(enabled)
    }

    override fun setRawDrawingRenderEnabled(enabled: Boolean) {
        onyxTouchHelper.setRawDrawingRenderEnabled(enabled)
    }

    override fun setStrokeWidth(width: Float) {
        onyxTouchHelper.setStrokeWidth(width)
    }

    override fun setStrokeStyle(style: Int) {
        onyxTouchHelper.setStrokeStyle(style)
    }

    override fun setLimitRect(limit: Rect, excludeRects: List<Rect>): BaseTouchHelper {
        onyxTouchHelper.setLimitRect(limit, ArrayList(excludeRects))
        return this
    }

    override fun openRawDrawing(): BaseTouchHelper {
        onyxTouchHelper.openRawDrawing()
        return this
    }

    override fun closeRawDrawing() {
        onyxTouchHelper.closeRawDrawing()
    }

    override fun cleanup() {
        onyxTouchHelper.closeRawDrawing()
    }
}