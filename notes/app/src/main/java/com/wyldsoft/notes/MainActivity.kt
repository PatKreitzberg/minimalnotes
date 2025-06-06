package com.wyldsoft.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wyldsoft.notes.backend.database.DatabaseManager
import com.wyldsoft.notes.navigation.AppView
import com.wyldsoft.notes.navigation.NavigationManager
import com.wyldsoft.notes.editorview.drawing.onyx.OnyxDrawingActivity
import com.wyldsoft.notes.editorview.editor.EditorState.Companion.setEraserMode
import com.wyldsoft.notes.ui.theme.MinimaleditorTheme
import com.wyldsoft.notes.ui.views.HomeView
import com.wyldsoft.notes.editorview.editor.EditorView
import com.wyldsoft.notes.ui.viewmodels.HomeViewModel
import com.wyldsoft.notes.ui.viewmodels.EditorViewModel

/**
 * Main activity that handles navigation between HomeView and EditorView
 * This class extends the Onyx-specific implementation for drawing capabilities including erasing
 */
class MainActivity : OnyxDrawingActivity() {
    private lateinit var navigationManager: NavigationManager
    private lateinit var databaseManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize navigation and database
        navigationManager = NavigationManager.getInstance()
        databaseManager = DatabaseManager.getInstance(this)

        setContent {
            MinimaleditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val currentView by remember { derivedStateOf { navigationManager.currentView } }

        LaunchedEffect(currentView) {
            when (currentView) {
                AppView.Home -> {
                    // Prepare for home view - disable drawing and enable finger touch
                    prepareForHomeView()
                }
                AppView.Editor -> {
                    // Editor view will handle its own touch setup
                }
            }
        }

        when (currentView) {
            AppView.Home -> {
                HomeView(
                    viewModel = viewModel(
                        factory = HomeViewModel.Factory(
                            repository = databaseManager.repository,
                            navigationManager = navigationManager
                        )
                    )
                )
            }
            AppView.Editor -> {
                // Set the current note in the drawing activity
                LaunchedEffect(navigationManager.currentNote) {
                    navigationManager.currentNote?.let { note ->
                        setCurrentNote(note)
                    }
                }

                EditorView(
                    viewModel = viewModel(
                        factory = EditorViewModel.Factory(
                            repository = databaseManager.repository,
                            navigationManager = navigationManager
                        )
                    ),
                    onSurfaceViewCreated = { surfaceView ->
                        handleSurfaceViewCreated(surfaceView)
                    },
                    onPenProfileChanged = { penProfile ->
                        updatePenProfile(penProfile)
                    },
                    onEraserModeChanged = { eraserEnabled ->
                        updateEraserMode(eraserEnabled)
                    },
                    onNavigateBack = {
                        // Force enable finger touch before navigating back
                        enableFingerTouch()
                        // This will be handled by the BackHandler in EditorView
                        // and the EditorViewModel.navigateBack() call
                    }
                )
            }
        }
    }

    // Override onBackPressed to handle navigation
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (navigationManager.currentView) {
            AppView.Editor -> {
                navigationManager.navigateBack()
            }
            AppView.Home -> {
                if (navigationManager.currentFolder != null) {
                    // Navigate to parent folder
                    // This will be handled by the HomeViewModel
                } else {
                    // Exit app
                    super.onBackPressed()
                }
            }
        }
    }

    // Method to handle eraser mode changes from toolbar
    override fun updateEraserMode(enabled: Boolean) {
        setEraserMode(enabled)
    }

    companion object {
        /**
         * Factory method to create the appropriate drawing activity
         * based on device type or configuration
         */
        private const val TAG = "MainActivity"
        fun createForDevice(): Class<out MainActivity> {
            // Future: Add device detection logic here
            // For now, always return Onyx implementation
            return MainActivity::class.java
        }
    }
}