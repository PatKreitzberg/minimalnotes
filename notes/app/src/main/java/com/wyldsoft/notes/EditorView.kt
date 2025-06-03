package com.wyldsoft.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorView(
    onSurfaceViewCreated: (android.view.SurfaceView) -> Unit = {}
) {
    val editorState = remember { EditorState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Minimal Android Editor with Onyx SDK",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Toolbar with stroke options panel
        Toolbar(editorState = editorState)

        Spacer(modifier = Modifier.height(16.dp))

        // Drawing canvas with real Onyx SDK integration
        DrawingCanvas(
            editorState = editorState,
            onSurfaceViewCreated = onSurfaceViewCreated
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Debug information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Debug Information",
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Drawing State: ${if (editorState.isDrawing) "Active" else "Inactive"}",
                    fontSize = 12.sp
                )
                Text(
                    text = "Exclusion Zones: ${editorState.stateExcludeRects.keys.joinToString()}",
                    fontSize = 12.sp
                )
                Text(
                    text = "Exclusions Modified: ${editorState.stateExcludeRectsModified}",
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How to test:",
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "1. Tap a pen button to open stroke options\n" +
                            "2. Adjust stroke size and color (not yet connected to Onyx SDK)\n" +
                            "3. Draw on the canvas with stylus (panel should close)\n" +
                            "4. Watch debug info and logs for state coordination",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
