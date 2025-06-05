package com.wyldsoft.notes.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "notebook_note_references",
    primaryKeys = ["notebookId", "noteId"],
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["notebookId"]),
        Index(value = ["noteId"])
    ]
)
data class NotebookNoteReference(
    val notebookId: String,
    val noteId: String,
    val addedAt: Long = System.currentTimeMillis()
)
