package com.wyldsoft.notes.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.wyldsoft.notes.DrawingCanvas
import com.wyldsoft.notes.ui.components.UpdatedToolbar
import com.wyldsoft.notes.pen.PenProfile

@Composable
fun EditorView(
    onSurfaceViewCreated: (android.view.SurfaceView) -> Unit = {},
    onPenProfileChanged: (PenProfile) -> Unit = {}
) {
    val editorState = remember { EditorState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Drawing App with Profile System",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Updated toolbar with 5 profiles
        UpdatedToolbar(
            editorState = editorState,
            onPenProfileChanged = onPenProfileChanged
        )

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
                    text = "1. Select a profile (1-5) by tapping its icon\n" +
                            "2. Tap the same profile again to open options panel\n" +
                            "3. Change pen type, stroke size, and color within the profile\n" +
                            "4. Draw on the canvas with stylus (panel should close)\n" +
                            "5. Each profile remembers its own settings\n" +
                            "6. Switch between profiles to use different pen configurations",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}