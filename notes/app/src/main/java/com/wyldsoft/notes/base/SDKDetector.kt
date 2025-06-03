package com.wyldsoft.notes.base

import android.content.Context
import android.os.Build

object SDKDetector {

    /**
     * Detect which SDK should be used based on device characteristics
     */
    fun detectSDKType(context: Context): SDKType {
        return when {
            isOnyxDevice() -> SDKType.ONYX
            isHuionDevice() -> SDKType.HUION
            isWacomDevice() -> SDKType.WACOM
            else -> SDKType.GENERIC
        }
    }

    private fun isOnyxDevice(): Boolean {
        // Check for Onyx device characteristics
        return Build.MANUFACTURER.contains("onyx", ignoreCase = true) ||
                Build.BRAND.contains("onyx", ignoreCase = true) ||
                hasOnyxSystemProperties()
    }

    private fun isHuionDevice(): Boolean {
        // Future: Add Huion device detection logic
        return Build.MANUFACTURER.contains("huion", ignoreCase = true) ||
                Build.BRAND.contains("huion", ignoreCase = true)
    }

    private fun isWacomDevice(): Boolean {
        // Future: Add Wacom device detection logic
        return Build.MANUFACTURER.contains("wacom", ignoreCase = true) ||
                Build.BRAND.contains("wacom", ignoreCase = true)
    }

    private fun hasOnyxSystemProperties(): Boolean {
        // Check for Onyx-specific system properties
        try {
            val process = Runtime.getRuntime().exec("getprop ro.onyx.version")
            val result = process.inputStream.bufferedReader().readText()
            return result.isNotEmpty()
        } catch (e: Exception) {
            return false
        }
    }
}