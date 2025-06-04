package com.wyldsoft.notes.ui.components

import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyldsoft.notes.ExcludeRects
import com.wyldsoft.notes.PenIconUtils
import kotlinx.coroutines.launch

import com.wyldsoft.notes.editor.EditorState
import com.wyldsoft.notes.pen.PenProfile


@Composable
fun UpdatedToolbar(
    editorState: EditorState,
    onPenProfileChanged: (PenProfile) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedProfileIndex by remember { mutableStateOf(0) } // Default to leftmost (index 0)
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var strokePanelRect by remember { mutableStateOf<Rect?>(null) }

    // Add a flag to track if we're waiting for panel to close
    var isPanelClosing by remember { mutableStateOf(false) }

    // Add a callback for when panel is fully removed
    var onPanelRemoved: (() -> Unit)? by remember { mutableStateOf(null) }

    // Store 5 profiles
    var profiles by remember {
        mutableStateOf(PenProfile.createDefaultProfiles())
    }

    // Current profile
    val currentProfile = profiles[selectedProfileIndex]

    // Force refresh counter for debugging
    var refreshCounter by remember { mutableStateOf(0) }

    fun forceUIRefresh() {
        refreshCounter++
        scope.launch {
            EditorState.refreshUi.emit(Unit)
        }
        Log.d("Toolbar:", "UI Refresh triggered: $refreshCounter")
    }

    fun addStrokeOptionPanelRect() {
        strokePanelRect?.let { rect ->
            editorState.stateExcludeRects[ExcludeRects.StrokeOptions] = rect
            editorState.stateExcludeRectsModified = true
            println("Added exclusion rect: $rect")

            val excludeRects = editorState.stateExcludeRects.values.toList()
            EditorState.updateExclusionZones(excludeRects)
            forceUIRefresh()
        }
    }

    fun removeStrokeOptionPanelRect() {
        editorState.stateExcludeRects.remove(ExcludeRects.StrokeOptions)
        editorState.stateExcludeRectsModified = true
        println("Removed exclusion rect")

        val excludeRects = editorState.stateExcludeRects.values.toList()
        EditorState.updateExclusionZones(excludeRects)
    }

    fun openStrokeOptionsPanel() {
        println("Opening stroke options panel for profile $selectedProfileIndex")
        isStrokeSelectionOpen = true
        isPanelClosing = false
        onPanelRemoved = null
    }

    fun closeStrokeOptionsPanel() {
        println("Closing stroke options panel")
        isPanelClosing = true

        // Set up the callback for after panel is removed
        onPanelRemoved = {
            removeStrokeOptionPanelRect()
            forceUIRefresh()
            scope.launch {
                println("REFRESH: onPanelRemoved about to forceRefresh()")
                EditorState.isStrokeOptionsOpen.emit(false)
                EditorState.forceRefresh()
            }
            isPanelClosing = false
        }

        // Trigger the panel removal
        isStrokeSelectionOpen = false
    }

    fun handleProfileClick(profileIndex: Int) {
        if (selectedProfileIndex == profileIndex && isStrokeSelectionOpen) {
            // Same profile clicked - close panel
            closeStrokeOptionsPanel()
        } else if (selectedProfileIndex == profileIndex && !isStrokeSelectionOpen) {
            // Same profile clicked - open panel
            openStrokeOptionsPanel()
        } else {
            // Different profile - switch profile and update
            if (isStrokeSelectionOpen) {
                closeStrokeOptionsPanel()
            }
            selectedProfileIndex = profileIndex
            val newProfile = profiles[profileIndex]
            onPenProfileChanged(newProfile)
            EditorState.updatePenProfile(newProfile)
        }
    }

    fun updateProfile(newProfile: PenProfile) {
        val updatedProfiles = profiles.toMutableList()
        updatedProfiles[selectedProfileIndex] = newProfile
        profiles = updatedProfiles

        // Immediately apply the new profile
        onPenProfileChanged(newProfile)
        EditorState.updatePenProfile(newProfile)

        println("Profile $selectedProfileIndex updated: $newProfile")
    }

    // Listen for drawing events to close panel
    LaunchedEffect(Unit) {
        launch {
            EditorState.drawingStarted.collect {
                if (isStrokeSelectionOpen) {
                    println("Drawing started - closing stroke options panel")
                    closeStrokeOptionsPanel()
                }
            }
        }

        launch {
            EditorState.forceScreenRefresh.collect {
                println("REFRESH: Force screen refresh requested")
                forceUIRefresh()
            }
        }
    }

    // Monitor drawing state changes
    LaunchedEffect(editorState.isDrawing) {
        if (editorState.isDrawing && isStrokeSelectionOpen) {
            println("REFRESH: Drawing started - closing stroke options panel")
            closeStrokeOptionsPanel()
        }
    }

    // Emit stroke options state changes
    LaunchedEffect(isStrokeSelectionOpen) {
        if (!isPanelClosing) {  // Only emit if not in the middle of closing
            EditorState.isStrokeOptionsOpen.emit(isStrokeSelectionOpen)
        }
    }

    // Handle exclusion rect changes
    LaunchedEffect(editorState.stateExcludeRectsModified) {
        if (editorState.stateExcludeRectsModified) {
            println("Exclusion rects modified - current zones: ${editorState.stateExcludeRects.keys}")
            editorState.stateExcludeRectsModified = false
            if (!isPanelClosing) {  // Only refresh if not waiting for panel to close
                forceUIRefresh()
            }
        }
    }

    // Initialize with default profile
    LaunchedEffect(Unit) {
        onPenProfileChanged(currentProfile)
        EditorState.updatePenProfile(currentProfile)
    }

    Column {
        // Main toolbar - single row with 5 profile buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profiles:", color = Color.Black, fontSize = 12.sp)

            // 5 Profile buttons
            profiles.forEachIndexed { index, profile ->
                ProfileButton(
                    profile = profile,
                    isSelected = selectedProfileIndex == index,
                    onClick = { handleProfileClick(index) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Debug info
            Text(
                text = "Profile: ${selectedProfileIndex + 1} | ${currentProfile.penType.displayName} | Refresh: $refreshCounter",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // Stroke options panel with disposal detection
        if (isStrokeSelectionOpen) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                DisposableEffect(Unit) {
                    onDispose {
                        // This runs when the panel is actually removed from composition
                        println("StrokeOptionsPanel removed from composition")
                        onPanelRemoved?.invoke()
                        onPanelRemoved = null
                    }
                }

                UpdatedStrokeOptionsPanel(
                    currentProfile = currentProfile,
                    onProfileChanged = { newProfile ->
                        updateProfile(newProfile)
                    },
                    onPanelPositioned = { rect ->
                        if (rect != strokePanelRect) {
                            strokePanelRect = rect
                            if (isStrokeSelectionOpen && !isPanelClosing) {
                                scope.launch {
                                    addStrokeOptionPanelRect()
                                }
                            }
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
    onClick: () -> Unit
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
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Icon(
            imageVector = PenIconUtils.getIconForPenType(profile.penType),
            contentDescription = PenIconUtils.getContentDescriptionForPenType(profile.penType),
            modifier = Modifier.size(24.dp)
        )
    }
}