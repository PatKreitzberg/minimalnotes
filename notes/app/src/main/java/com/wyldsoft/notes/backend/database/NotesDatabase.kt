package com.wyldsoft.notes.backend.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wyldsoft.notes.backend.database.entities.*
import com.wyldsoft.notes.backend.database.dao.*
import com.wyldsoft.notes.backend.database.converters.*

@Database(
    entities = [
        Folder::class,
        Notebook::class,
        Note::class,
        NotebookNoteReference::class,
        Shape::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(TouchPointListConverter::class, PenProfileConverter::class)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun notebookDao(): NotebookDao
    abstract fun noteDao(): NoteDao
    abstract fun shapeDao(): ShapeDao

    companion object {
        @Volatile
        private var INSTANCE: NotesDatabase? = null

        fun getDatabase(context: Context): NotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Create default root folder when database is first created
            db.execSQL("""
                INSERT INTO folders (id, name, parentFolderId, createdAt, updatedAt) 
                VALUES ('root', 'Root', NULL, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """)
        }
    }
}

