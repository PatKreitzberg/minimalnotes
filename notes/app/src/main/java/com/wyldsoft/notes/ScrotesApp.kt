package com.wyldsoft.notes

import android.app.Application
import android.os.Build
import com.onyx.android.sdk.rx.RxBaseAction
import com.onyx.android.sdk.utils.ResManager
import com.wyldsoft.notes.backend.database.DatabaseManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class ScrotesApp : Application() {

    // Initialize database manager
    val databaseManager by lazy { DatabaseManager.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        ResManager.init(this)
        RxBaseAction.init(this)
        checkHiddenApiBypass()

        // Initialize database
        databaseManager.repository
    }

    private fun checkHiddenApiBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }
}