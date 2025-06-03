package com.wyldsoft.notes.base

import android.content.Context

abstract class BaseDeviceReceiver {
    abstract fun enable(context: Context, enable: Boolean)
    abstract fun setSystemNotificationPanelChangeListener(listener: (Boolean) -> Unit): BaseDeviceReceiver
    abstract fun setSystemScreenOnListener(listener: () -> Unit): BaseDeviceReceiver
    abstract fun cleanup()
}