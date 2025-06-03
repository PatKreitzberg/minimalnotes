package com.wyldsoft.notes

import android.graphics.Rect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun Toolbar(
    editorState: EditorState
) {
    val scope = rememberCoroutineScope()
    var selectedPen by remember { mutableStateOf(PenType.BALLPEN) }
    var isStrokeSelectionOpen by remember { mutableStateOf(false) }
    var strokePanelRect by remember { mutableStateOf<Rect?>(null) }
    var penSettings by remember {
        mutableStateOf(
            mapOf(
                PenType.BALLPEN to PenSetting(5f, Color.Black),
                PenType.FOUNTAIN to PenSetting(8f, Color.Black),
                PenType.MARKER to PenSetting(20f, Color.Gray)
            )
        )
    }

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
            // Different pen - switch pen and close panel if open
            if (isStrokeSelectionOpen) {
                closeStrokeOptionsPanel()
            }
            selectedPen = pen
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

    // Close panel when drawing starts (detect from drawing notifications)
    DisposableEffect(Unit) {
        val originalNotifyDrawingStarted = EditorState.Companion::notifyDrawingStarted

        // Override the companion object method to detect drawing start
        EditorState.Companion.apply {
            // Note: This is a simplified approach. In real implementation,
            // you might use a flow or callback mechanism
        }

        onDispose {
            // Cleanup if needed
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

    Column {
        // Main toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(1.dp, Color.Gray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tools:", color = Color.Black)

            // Pen buttons
            ToolbarButton(
                text = "Ball",
                icon = Icons.Default.Create,
                isSelected = selectedPen == PenType.BALLPEN,
                penColor = penSettings[PenType.BALLPEN]?.color,
                onClick = { handlePenClick(PenType.BALLPEN) }
            )

            ToolbarButton(
                text = "Fountain",
                icon = Icons.Default.Create,
                isSelected = selectedPen == PenType.FOUNTAIN,
                penColor = penSettings[PenType.FOUNTAIN]?.color,
                onClick = { handlePenClick(PenType.FOUNTAIN) }
            )

            ToolbarButton(
                text = "Marker",
                icon = Icons.Default.Build,
                isSelected = selectedPen == PenType.MARKER,
                penColor = penSettings[PenType.MARKER]?.color,
                onClick = { handlePenClick(PenType.MARKER) }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Debug info
            Text(
                text = "Drawing: ${editorState.isDrawing} | Exclusions: ${editorState.stateExcludeRects.size} | Refresh: $refreshCounter",
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
                    currentSetting = penSettings[selectedPen] ?: PenSetting(5f, Color.Black),
                    onSettingChanged = { newSetting ->
                        val updatedSettings = penSettings.toMutableMap()
                        updatedSettings[selectedPen] = newSetting
                        penSettings = updatedSettings
                        println("Pen settings updated: $selectedPen -> $newSetting")
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
        modifier = Modifier.height(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp)
    }
}
