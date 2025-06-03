// MainActivity.kt - Refactored
package com.wyldsoft.notes

import com.wyldsoft.notes.onyx.OnyxDrawingActivity

/**
 * Main activity that uses Onyx SDK implementation.
 * This class now simply extends the Onyx-specific implementation.
 *
 * To support a different SDK in the future, you would:
 * 1. Create a new implementation like HuionDrawingActivity
 * 2. Change this class to extend that implementation instead
 * 3. Or use a factory pattern to choose the implementation at runtime
 */
class MainActivity : OnyxDrawingActivity() {

    companion object {
        /**
         * Factory method to create the appropriate drawing activity
         * based on device type or configuration
         */
        fun createForDevice(): Class<out MainActivity> {
            // Future: Add device detection logic here
            // For now, always return Onyx implementation
            return MainActivity::class.java
        }
    }

    // MainActivity can add any app-specific functionality here
    // while inheriting all the drawing capabilities from OnyxDrawingActivity
}