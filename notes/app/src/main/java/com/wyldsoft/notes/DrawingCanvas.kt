package com.wyldsoft.notes

import android.view.SurfaceView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun DrawingCanvas(
    editorState: EditorState,
    onSurfaceViewCreated: (SurfaceView) -> Unit
) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                // Surface will be configured by the activity
                onSurfaceViewCreated(this)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    )
}
