package com.wyldsoft.notes.editorview.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.wyldsoft.notes.editorview.ui.components.DrawingCanvas
import com.wyldsoft.notes.editorview.ui.components.UpdatedToolbar
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.ui.viewmodels.EditorViewModel
import com.wyldsoft.notes.ui.gestures.SwipeBackGestureWrapper

/**
 * Simplified EditorView with clean interface focused on drawing
 * Features toolbar and full-screen drawing canvas with swipe back navigation
 */
@Composable
fun EditorView(
    viewModel: EditorViewModel = viewModel(),
    onSurfaceViewCreated: (android.view.SurfaceView) -> Unit = {},
    onPenProfileChanged: (PenProfile) -> Unit = {},
    onEraserModeChanged: (Boolean) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val editorState = remember { EditorState() }
    val currentNote by viewModel.currentNote.collectAsState()
    val error by viewModel.error.collectAsState()

    // Handle back navigation function
    val handleBackNavigation = {
        onNavigateBack()
        viewModel.navigateBack()
    }

    // Handle back button
    BackHandler {
        handleBackNavigation()
    }

    // Handle errors
    LaunchedEffect(error) {
        error?.let {
            viewModel.clearError()
        }
    }

    // Update current note when navigation changes
    LaunchedEffect(Unit) {
        viewModel.updateCurrentNote()
    }

    // Listen for erasing events to update UI state
    LaunchedEffect(Unit) {
        launch {
            EditorState.erasingStarted.collect {
                editorState.isErasing = true
            }
        }

        launch {
            EditorState.erasingEnded.collect {
                editorState.isErasing = false
            }
        }

        launch {
            EditorState.drawingStarted.collect {
                editorState.isDrawing = true
            }
        }

        launch {
            EditorState.drawingEnded.collect {
                editorState.isDrawing = false
            }
        }
    }

    // Wrap entire editor in swipe gesture handler
    SwipeBackGestureWrapper(
        onSwipeBack = handleBackNavigation,
        enabled = !editorState.isDrawing && !editorState.isErasing // Disable swipe when actively drawing or erasing
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content column with toolbar and canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp) // No top padding - toolbar flush with top
            ) {
                // Clean toolbar without debug text
                CleanToolbar(
                    editorState = editorState,
                    onPenProfileChanged = onPenProfileChanged,
                    onEraserModeChanged = onEraserModeChanged
                )

                Spacer(modifier = Modifier.height(8.dp)) // Reduced spacing

                // Expanded drawing canvas that fills remaining space
                DrawingCanvas(
                    editorState = editorState,
                    onSurfaceViewCreated = onSurfaceViewCreated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes all remaining space
                )
            }
        }
    }
}

/**
 * Clean toolbar component without debug information
 * Provides pen profiles and eraser functionality in minimal space
 */
@Composable
private fun CleanToolbar(
    editorState: EditorState,
    onPenProfileChanged: (PenProfile) -> Unit,
    onEraserModeChanged: (Boolean) -> Unit
) {
    UpdatedToolbar(
        editorState = editorState,
        onPenProfileChanged = onPenProfileChanged,
        onEraserModeChanged = onEraserModeChanged
    )
}