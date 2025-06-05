package com.wyldsoft.notes.database

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.wyldsoft.notes.database.repository.NotesRepository

/**
 * Manager class to initialize and provide database dependencies
 */
class DatabaseManager private constructor(context: Context) {

    private val database = NotesDatabase.getDatabase(context)
    val repository = NotesRepository.getInstance(database)

    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}