package com.wyldsoft.notes.base

abstract class BaseDrawingCallback {
    abstract fun onBeginRawDrawing(isSuccessful: Boolean, point: Any?)
    abstract fun onEndRawDrawing(isSuccessful: Boolean, point: Any?)
    abstract fun onRawDrawingTouchPointMoveReceived(point: Any?)
    abstract fun onRawDrawingTouchPointListReceived(pointList: Any?)
    abstract fun onBeginRawErasing(isSuccessful: Boolean, point: Any?)
    abstract fun onEndRawErasing(isSuccessful: Boolean, point: Any?)
    abstract fun onRawErasingTouchPointMoveReceived(point: Any?)
    abstract fun onRawErasingTouchPointListReceived(pointList: Any?)
}