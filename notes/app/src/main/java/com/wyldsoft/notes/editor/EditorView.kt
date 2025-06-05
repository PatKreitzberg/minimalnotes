package com.wyldsoft.notes.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wyldsoft.notes.DrawingCanvas
import com.wyldsoft.notes.ui.components.UpdatedToolbar
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.ui.viewmodels.EditorViewModel
import com.wyldsoft.notes.ui.gestures.SwipeBackGestureWrapper

/**
 * EditorView for drawing with navigation support and swipe back gesture
 */
@Composable
fun EditorView(
    viewModel: EditorViewModel = viewModel(),
    onSurfaceViewCreated: (android.view.SurfaceView) -> Unit = {},
    onPenProfileChanged: (PenProfile) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val editorState = remember { EditorState() }
    val currentNote by viewModel.currentNote.collectAsState()
    val currentNotebook by viewModel.currentNotebook.collectAsState()
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

    // Wrap entire editor in swipe gesture handler
    SwipeBackGestureWrapper(
        onSwipeBack = handleBackNavigation,
        enabled = !editorState.isDrawing // Disable swipe when actively drawing
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title bar with back button and note info
            EditorTitleBar(
                notebookName = currentNotebook?.name ?: "Unknown Notebook",
                noteName = currentNote?.title ?: "Untitled Note",
                onBackClick = handleBackNavigation,
                onTitleChange = { newTitle ->
                    viewModel.saveNoteTitle(newTitle)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Drawing toolbar with 5 profiles
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
                        text = "Drawing Information",
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notebook: ${currentNotebook?.name ?: "Unknown"}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Note: ${currentNote?.title ?: "Untitled"}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Note ID: ${currentNote?.id ?: "Unknown"}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Drawing State: ${if (editorState.isDrawing) "Active" else "Inactive"}",
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Navigation:",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Tap back button to return to home\n" +
                                "• Swipe right from left edge to go back\n" +
                                "• Swipe gesture disabled while drawing\n" +
                                "• Your drawings are automatically saved",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorTitleBar(
    notebookName: String,
    noteName: String,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(noteName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with back button and notebook name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Notebook:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = notebookName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Note:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isEditingTitle) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            onTitleChange(editedTitle.trim())
                            isEditingTitle = false
                        }
                    ) {
                        Text("Save", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = {
                            editedTitle = noteName
                            isEditingTitle = false
                        }
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = noteName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            editedTitle = noteName
                            isEditingTitle = true
                        }
                    ) {
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}