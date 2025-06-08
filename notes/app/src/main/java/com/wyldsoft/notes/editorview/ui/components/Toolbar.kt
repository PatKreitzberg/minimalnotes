package com.wyldsoft.notes.editorview.ui.components

import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyldsoft.notes.ExcludeRects
import com.wyldsoft.notes.PenIconUtils
import kotlinx.coroutines.launch

import com.wyldsoft.notes.editorview.editor.EditorState
import com.wyldsoft.notes.pen.PenProfile

/**
 * Clean toolbar with 5 pen profiles and an eraser button
 * FIXED: Proper exclusion zone coordinate handling and cleanup
 */
@Composable
fun UpdatedToolbar(
    editorState: EditorState,
    onPenProfileChanged: (PenProfile) -> Unit = {},
    onEraserModeChanged: (Boolean) -> Unit = {},
    onZoomToFit: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var selectedProfileIndex by remember { mutableStateOf(0) }
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var eraserModeEnabled by remember { mutableStateOf(false) }

    // Critical fix: Track panel cleanup state properly
    var isPanelFullyRemoved by remember { mutableStateOf(true) }
    var pendingCleanup by remember { mutableStateOf(false) }

    // Store 5 profiles
    var profiles by remember {
        mutableStateOf(PenProfile.createDefaultProfiles())
    }

    val currentProfile = profiles[selectedProfileIndex]

    // FIXED: Proper exclusion zone management functions
    fun addStrokeOptionPanelRect(rect: Rect) {
        Log.d("Toolbar", "Adding exclusion rect: $rect")
        editorState.stateExcludeRects[ExcludeRects.StrokeOptions] = rect
        editorState.stateExcludeRectsModified = true

        val excludeRects = editorState.stateExcludeRects.values.toList()
        Log.d("Toolbar", "Total exclusion rects: ${excludeRects.size}")
        EditorState.updateExclusionZones(excludeRects)
    }

    fun removeStrokeOptionPanelRect() {
        Log.d("Toolbar", "Removing exclusion rect")
        editorState.stateExcludeRects.remove(ExcludeRects.StrokeOptions)

        editorState.stateExcludeRectsModified = true

        val excludeRects = editorState.stateExcludeRects.values.toList()
        Log.d("Toolbar", "Remaining exclusion rects: ${excludeRects.size}")
        EditorState.updateExclusionZones(excludeRects)

        // Force a refresh to clear any lingering exclusion zones
        scope.launch {
            EditorState.forceRefresh()
        }
    }

    fun openStrokeOptionsPanel() {
        Log.d("Toolbar", "Opening stroke options panel")
        isPanelFullyRemoved = false
        pendingCleanup = false
        isStrokeSelectionOpen = true
    }

    fun closeStrokeOptionsPanel() {
        Log.d("Toolbar", "Closing stroke options panel")
        pendingCleanup = true
        isStrokeSelectionOpen = false

        // Immediate cleanup - don't wait for composition
        removeStrokeOptionPanelRect()
    }

    fun handleProfileClick(profileIndex: Int) {
        // Exit eraser mode when selecting a pen profile
        if (eraserModeEnabled) {
            eraserModeEnabled = false
            editorState.eraserMode = false
            onEraserModeChanged(false)
            EditorState.setEraserMode(false)
        }

        if (selectedProfileIndex == profileIndex && isStrokeSelectionOpen) {
            closeStrokeOptionsPanel()
        } else if (selectedProfileIndex == profileIndex && !isStrokeSelectionOpen) {
            openStrokeOptionsPanel()
        } else {
            if (isStrokeSelectionOpen) {
                closeStrokeOptionsPanel()
            }
            selectedProfileIndex = profileIndex
            val newProfile = profiles[profileIndex]
            onPenProfileChanged(newProfile)
            EditorState.updatePenProfile(newProfile)
        }
    }

    fun handleEraserClick() {
        if (isStrokeSelectionOpen) {
            closeStrokeOptionsPanel()
        }

        eraserModeEnabled = !eraserModeEnabled
        editorState.eraserMode = eraserModeEnabled
        onEraserModeChanged(eraserModeEnabled)
        EditorState.setEraserMode(eraserModeEnabled)
    }

    fun updateProfile(newProfile: PenProfile) {
        val updatedProfiles = profiles.toMutableList()
        updatedProfiles[selectedProfileIndex] = newProfile
        profiles = updatedProfiles

        onPenProfileChanged(newProfile)
        EditorState.updatePenProfile(newProfile)
    }

    // Listen for drawing/erasing events to close panel
    LaunchedEffect(Unit) {
        launch {
            EditorState.drawingStarted.collect {
                if (isStrokeSelectionOpen) {
                    closeStrokeOptionsPanel()
                }
            }
        }

        launch {
            EditorState.erasingStarted.collect {
                if (isStrokeSelectionOpen) {
                    closeStrokeOptionsPanel()
                }
            }
        }
    }

    // Monitor state changes and force cleanup
    LaunchedEffect(editorState.isDrawing, editorState.isErasing) {
        if ((editorState.isDrawing || editorState.isErasing) && isStrokeSelectionOpen) {
            closeStrokeOptionsPanel()
        }
    }

    // Ensure cleanup happens when panel closes
    LaunchedEffect(isStrokeSelectionOpen) {
        if (!isStrokeSelectionOpen && !isPanelFullyRemoved) {
            Log.d("Toolbar", "Panel closed, marking as fully removed")
            isPanelFullyRemoved = true
            pendingCleanup = false
        }
    }

    // Initialize with default profile
    LaunchedEffect(Unit) {
        onPenProfileChanged(currentProfile)
        EditorState.updatePenProfile(currentProfile)
    }

    Box {
        // Main toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), // Reduced spacing
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tools:", color = Color.Black, fontSize = 12.sp)

            // 5 Profile buttons (20% smaller)
            profiles.forEachIndexed { index, profile ->
                ProfileButton(
                    profile = profile,
                    isSelected = selectedProfileIndex == index && !eraserModeEnabled,
                    onClick = { handleProfileClick(index) },
                    size = 38.dp, // 20% smaller than 48dp
                    iconSize = 19.dp // 20% smaller than 24dp
                )
            }

            // Vertical divider line
            VerticalDivider()

            // Eraser button (20% smaller)
            EraserButton(
                isSelected = eraserModeEnabled,
                onClick = { handleEraserClick() },
                size = 38.dp,
                iconSize = 19.dp
            )

            // Vertical divider line
            VerticalDivider()

            // Zoom to 100% button
            NavigationButton(
                icon = Icons.Default.CenterFocusStrong,
                contentDescription = "Zoom to 100%",
                onClick = onZoomToFit,
                size = 38.dp,
                iconSize = 19.dp
            )
        }

        if (isStrokeSelectionOpen && !eraserModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp)
            ) {
                // Track when panel is actually removed from composition
                DisposableEffect(Unit) {
                    onDispose {
                        Log.d("Toolbar", "Panel disposed from composition")
                        if (pendingCleanup) {
                            removeStrokeOptionPanelRect()
                        }
                    }
                }

                UpdatedStrokeOptionsPanel(
                    currentProfile = currentProfile,
                    onProfileChanged = { newProfile ->
                        updateProfile(newProfile)
                    },
                    onPanelPositioned = { bounds ->
                        Log.d("Toolbar", "Panel positioned at: $bounds")

                        // CRITICAL FIX: Don't apply exclusion zone if panel is being closed
                        if (isStrokeSelectionOpen && !pendingCleanup) {
                            // Convert from window coordinates to surface coordinates properly
                            // The bounds from boundsInWindow are already in screen pixels
                            val surfaceRect = Rect(
                                bounds.left,
                                bounds.top,
                                bounds.right,
                                bounds.bottom
                            )

                            addStrokeOptionPanelRect(surfaceRect)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileButton(
    profile: PenProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) profile.strokeColor else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.Black else Color.Gray
        ),
        modifier = Modifier.size(size),
        contentPadding = PaddingValues(4.dp)
    ) {
        Icon(
            imageVector = PenIconUtils.getIconForPenType(profile.penType),
            contentDescription = PenIconUtils.getContentDescriptionForPenType(profile.penType),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color.Gray)
    )
}

@Composable
fun EraserButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Red else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Red
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.Red else Color.Gray
        ),
        modifier = Modifier.size(size),
        contentPadding = PaddingValues(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Eraser",
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun NavigationButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.Gray
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) Color.Gray else Color.LightGray
        ),
        modifier = Modifier.size(size),
        contentPadding = PaddingValues(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}