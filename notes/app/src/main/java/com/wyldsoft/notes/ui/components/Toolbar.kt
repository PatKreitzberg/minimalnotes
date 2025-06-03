package com.wyldsoft.notes.ui.components

import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyldsoft.notes.editor.EditorState
import com.wyldsoft.notes.editor.ExcludeRects
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.pen.PenType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun Toolbar(
    editorState: EditorState,
    onPenProfileChanged: (PenProfile) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedPen by remember { mutableStateOf(PenType.BALLPEN) }
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var strokePanelRect by remember { mutableStateOf<Rect?>(null) }

    // Store pen profiles for each pen type
    var penProfiles by remember {
        mutableStateOf(
            PenType.values().associateWith { penType ->
                PenProfile.getDefaultProfile(penType)
            }
        )
    }

    // Current pen profile
    val currentPenProfile = penProfiles[selectedPen] ?: PenProfile.getDefaultProfile(selectedPen)

    // Force refresh counter for debugging
    var refreshCounter by remember { mutableStateOf(0) }

    fun forceUIRefresh() {
        refreshCounter++
        scope.launch {
            EditorState.refreshUi.emit(Unit)
        }
        println("UI Refresh triggered: $refreshCounter")
    }

    fun addStrokeOptionPanelRect() {
        strokePanelRect?.let { rect ->
            editorState.stateExcludeRects[ExcludeRects.StrokeOptions] = rect
            editorState.stateExcludeRectsModified = true
            println("Added exclusion rect: $rect")

            // Update touch surface with new exclusion zones
            val excludeRects = editorState.stateExcludeRects.values.toList()
            EditorState.updateExclusionZones(excludeRects)

            forceUIRefresh()
        }
    }

    fun removeStrokeOptionPanelRect() {
        editorState.stateExcludeRects.remove(ExcludeRects.StrokeOptions)
        editorState.stateExcludeRectsModified = true
        isStrokeSelectionOpen = false
        println("Removed exclusion rect")

        // Update touch surface with cleared exclusion zones
        val excludeRects = editorState.stateExcludeRects.values.toList()
        EditorState.updateExclusionZones(excludeRects)

        forceUIRefresh()
    }

    fun openStrokeOptionsPanel() {
        println("Opening stroke options panel")
        isStrokeSelectionOpen = true
        // Delay adding exclusion rect until panel is positioned
        scope.launch {
            delay(50)
            addStrokeOptionPanelRect()
        }
    }

    fun closeStrokeOptionsPanel() {
        println("Closing stroke options panel")
        removeStrokeOptionPanelRect()
        scope.launch {
            EditorState.isStrokeOptionsOpen.emit(false)
        }
    }

    fun handlePenClick(pen: PenType) {
        if (selectedPen == pen && isStrokeSelectionOpen) {
            // Same pen clicked - close panel
            closeStrokeOptionsPanel()
        } else if (selectedPen == pen && !isStrokeSelectionOpen) {
            // Same pen clicked - open panel
            openStrokeOptionsPanel()
        } else {
            // Different pen - switch pen and update profile
            if (isStrokeSelectionOpen) {
                closeStrokeOptionsPanel()
            }
            selectedPen = pen
            // Immediately update the pen profile when switching pens
            val newProfile = penProfiles[pen] ?: PenProfile.getDefaultProfile(pen)
            onPenProfileChanged(newProfile)
            EditorState.updatePenProfile(newProfile)
        }
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
                println("Force screen refresh requested")
                forceUIRefresh()
            }
        }
    }

    // Monitor drawing state changes
    LaunchedEffect(editorState.isDrawing) {
        if (editorState.isDrawing && isStrokeSelectionOpen) {
            println("Drawing started - closing stroke options panel")
            closeStrokeOptionsPanel()
        }
    }

    // Emit stroke options state changes
    LaunchedEffect(isStrokeSelectionOpen) {
        EditorState.isStrokeOptionsOpen.emit(isStrokeSelectionOpen)
        if (!isStrokeSelectionOpen) {
            editorState.stateExcludeRects.remove(ExcludeRects.StrokeOptions)
            editorState.stateExcludeRectsModified = true
        }
    }

    // Handle exclusion rect changes
    LaunchedEffect(editorState.stateExcludeRectsModified) {
        if (editorState.stateExcludeRectsModified) {
            println("Exclusion rects modified - current zones: ${editorState.stateExcludeRects.keys}")
            editorState.stateExcludeRectsModified = false
            forceUIRefresh()
        }
    }

    // Initialize with default pen profile
    LaunchedEffect(Unit) {
        onPenProfileChanged(currentPenProfile)
        EditorState.updatePenProfile(currentPenProfile)
    }

    Column {
        // Main toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tools:", color = Color.Black, fontSize = 12.sp)

            // Pen buttons - first row
            ToolbarButton(
                text = "Ball",
                icon = Icons.Default.Create,
                isSelected = selectedPen == PenType.BALLPEN,
                penColor = penProfiles[PenType.BALLPEN]?.strokeColor,
                onClick = { handlePenClick(PenType.BALLPEN) }
            )

            ToolbarButton(
                text = "Fountain",
                icon = Icons.Default.Create,
                isSelected = selectedPen == PenType.FOUNTAIN,
                penColor = penProfiles[PenType.FOUNTAIN]?.strokeColor,
                onClick = { handlePenClick(PenType.FOUNTAIN) }
            )

            ToolbarButton(
                text = "Marker",
                icon = Icons.Default.Build,
                isSelected = selectedPen == PenType.MARKER,
                penColor = penProfiles[PenType.MARKER]?.strokeColor,
                onClick = { handlePenClick(PenType.MARKER) }
            )

            ToolbarButton(
                text = "Pencil",
                icon = Icons.Default.Edit,
                isSelected = selectedPen == PenType.PENCIL,
                penColor = penProfiles[PenType.PENCIL]?.strokeColor,
                onClick = { handlePenClick(PenType.PENCIL) }
            )
        }

        // Second row of pen buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("More:", color = Color.Black, fontSize = 12.sp)

            ToolbarButton(
                text = "Charcoal",
                icon = Icons.Default.Face,
                isSelected = selectedPen == PenType.CHARCOAL,
                penColor = penProfiles[PenType.CHARCOAL]?.strokeColor,
                onClick = { handlePenClick(PenType.CHARCOAL) }
            )

            ToolbarButton(
                text = "Charcoal V2",
                icon = Icons.Default.Face,
                isSelected = selectedPen == PenType.CHARCOAL_V2,
                penColor = penProfiles[PenType.CHARCOAL_V2]?.strokeColor,
                onClick = { handlePenClick(PenType.CHARCOAL_V2) }
            )

            ToolbarButton(
                text = "Neo Brush",
                icon = Icons.Default.Build,
                isSelected = selectedPen == PenType.NEO_BRUSH,
                penColor = penProfiles[PenType.NEO_BRUSH]?.strokeColor,
                onClick = { handlePenClick(PenType.NEO_BRUSH) }
            )

            ToolbarButton(
                text = "Dash",
                icon = Icons.Default.Create,
                isSelected = selectedPen == PenType.DASH,
                penColor = penProfiles[PenType.DASH]?.strokeColor,
                onClick = { handlePenClick(PenType.DASH) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Debug info
            Text(
                text = "Drawing: ${editorState.isDrawing} | Pen: ${selectedPen.displayName} | Refresh: $refreshCounter",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // Stroke options panel
        if (isStrokeSelectionOpen) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                StrokeOptionsPanel(
                    currentPen = selectedPen,
                    currentProfile = currentPenProfile,
                    onProfileChanged = { newProfile ->
                        val updatedProfiles = penProfiles.toMutableMap()
                        updatedProfiles[selectedPen] = newProfile
                        penProfiles = updatedProfiles

                        // Immediately apply the new profile
                        onPenProfileChanged(newProfile)
                        EditorState.updatePenProfile(newProfile)

                        println("Pen profile updated: $selectedPen -> $newProfile")
                    },
                    onPanelPositioned = { rect ->
                        if (rect != strokePanelRect) {
                            strokePanelRect = rect
                            if (isStrokeSelectionOpen) {
                                addStrokeOptionPanelRect()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ToolbarButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    penColor: Color?,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) (penColor ?: Color.Black) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color.Black else Color.Gray
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp)
    }
}