package com.wyldsoft.notes.editorview.ui.components

import android.view.SurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.wyldsoft.notes.editorview.editor.EditorState

/**
 * Drawing canvas component that adapts to available space
 * Provides the surface view for drawing operations
 */
@Composable
fun DrawingCanvas(
    editorState: EditorState,
    onSurfaceViewCreated: (SurfaceView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                // Surface will be configured by the activity
                onSurfaceViewCreated(this)
            }
        },
        modifier = modifier
    )
}