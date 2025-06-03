package com.wyldsoft.notes.base

import android.content.Context
import android.content.SharedPreferences

class DeviceConfig private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_config", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: DeviceConfig? = null

        fun getInstance(context: Context): DeviceConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var detectedSDKType: SDKType
        get() = SDKType.valueOf(prefs.getString("sdk_type", SDKType.ONYX.name) ?: SDKType.ONYX.name)
        set(value) = prefs.edit().putString("sdk_type", value.name).apply()

    var forceSDKType: SDKType?
        get() = prefs.getString("force_sdk_type", null)?.let { SDKType.valueOf(it) }
        set(value) = if (value != null) {
            prefs.edit().putString("force_sdk_type", value.name).apply()
        } else {
            prefs.edit().remove("force_sdk_type").apply()
        }

    fun getCurrentSDKType(context: Context): SDKType {
        return forceSDKType ?: run {
            val detected = SDKDetector.detectSDKType(context)
            detectedSDKType = detected
            detected
        }
    }

    // Performance settings
    var enableOptimizedRendering: Boolean
        get() = prefs.getBoolean("optimized_rendering", true)
        set(value) = prefs.edit().putBoolean("optimized_rendering", value).apply()

    var maxStrokeWidth: Float
        get() = prefs.getFloat("max_stroke_width", 60f)
        set(value) = prefs.edit().putFloat("max_stroke_width", value).apply()
}