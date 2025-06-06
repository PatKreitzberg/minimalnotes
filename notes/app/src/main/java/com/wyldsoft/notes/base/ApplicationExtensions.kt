package com.wyldsoft.notes.base

import android.app.Application
import android.content.Context
import com.wyldsoft.notes.editorview.drawing.base.BaseDrawingActivity

/**
 * Extension functions to help with the new architecture
 */
fun Application.initializeDrawingSDK(): SDKType {
    val config = DeviceConfig.getInstance(this)
    val sdkType = config.getCurrentSDKType(this)

    // Log the detected SDK
    android.util.Log.i("DrawingSDK", "Detected SDK: $sdkType")

    return sdkType
}

fun Context.getDrawingActivityClass(): Class<out BaseDrawingActivity> {
    val config = DeviceConfig.getInstance(this)
    val sdkType = config.getCurrentSDKType(this)
    return SDKFactory.createDrawingActivityClass(sdkType)
}