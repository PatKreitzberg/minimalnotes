package com.wyldsoft.notes.base

import android.content.Context
import com.wyldsoft.notes.MainActivity
import com.wyldsoft.notes.onyx.OnyxDrawingActivity

object SDKFactory {

    /**
     * Create the appropriate drawing activity class based on SDK type
     */
    fun createDrawingActivityClass(sdkType: SDKType): Class<out BaseDrawingActivity> {
        return when (sdkType) {
            SDKType.ONYX -> OnyxDrawingActivity::class.java
            SDKType.HUION -> {
                // Future: Return HuionDrawingActivity::class.java
                throw UnsupportedOperationException("Huion SDK not yet implemented")
            }
            SDKType.WACOM -> {
                // Future: Return WacomDrawingActivity::class.java
                throw UnsupportedOperationException("Wacom SDK not yet implemented")
            }
            SDKType.GENERIC -> {
                // Future: Return GenericDrawingActivity::class.java
                throw UnsupportedOperationException("Generic SDK not yet implemented")
            }
        }
    }

    /**
     * Get the appropriate MainActivity class for the current device
     */
    fun getMainActivityClass(context: Context): Class<out MainActivity> {
        val sdkType = SDKDetector.detectSDKType(context)
        return when (sdkType) {
            SDKType.ONYX -> MainActivity::class.java
            else -> {
                // For now, fallback to Onyx implementation
                // Future: Create device-specific MainActivity classes
                MainActivity::class.java
            }
        }
    }
}
