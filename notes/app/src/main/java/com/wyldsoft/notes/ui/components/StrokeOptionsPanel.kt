package com.wyldsoft.notes.ui.components

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wyldsoft.notes.editor.EditorState
import com.wyldsoft.notes.pen.PenProfile
import com.wyldsoft.notes.pen.PenType
import com.wyldsoft.notes.utils.noRippleClickable

@Composable
fun StrokeOptionsPanel(
    currentPen: PenType,
    currentProfile: PenProfile,
    onProfileChanged: (PenProfile) -> Unit,
    onPanelPositioned: (Rect) -> Unit = {}
) {
    var strokeSize by remember(currentProfile) { mutableStateOf(currentProfile.strokeWidth) }
    var selectedColor by remember(currentProfile) { mutableStateOf(currentProfile.strokeColor) }
    val density = LocalDensity.current

    // Calculate the maximum stroke size based on pen type
    val maxStrokeSize = when (currentPen) {
        PenType.BALLPEN -> 20f
        PenType.FOUNTAIN -> 30f
        PenType.MARKER -> 60f
        PenType.PENCIL -> 15f
        PenType.CHARCOAL -> 40f
        PenType.CHARCOAL_V2 -> 40f
        PenType.NEO_BRUSH -> 50f
        PenType.DASH -> 25f
    }

    // Apply settings immediately when they change
    LaunchedEffect(strokeSize, selectedColor) {
        val newProfile = PenProfile(strokeSize, currentPen, selectedColor)
        onProfileChanged(newProfile)
        EditorState.refreshUi.emit(Unit)
    }

    Column(
        modifier = Modifier
            .wrapContentWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .padding(16.dp)
            .onGloballyPositioned { coordinates ->
                val boundingRect = coordinates.boundsInWindow()

                val panelRect = Rect(
                    with(density) { boundingRect.left.toDp().value.toInt() },
                    with(density) { boundingRect.top.toDp().value.toInt() },
                    with(density) { boundingRect.right.toDp().value.toInt() },
                    with(density) { boundingRect.bottom.toDp().value.toInt() }
                )

                println("StrokeOptionsPanel positioned: $panelRect")
                onPanelPositioned(panelRect)
            }
    ) {
        // Header with pen name and preview
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "${currentPen.displayName} Options",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            // Stroke preview
            Box(
                modifier = Modifier
                    .size(strokeSize.dp.coerceAtLeast(8.dp))
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(1.dp, Color.Gray, CircleShape)
            )
        }

        // Stroke size slider
        Text(text = "Stroke Size: ${strokeSize.toInt()}")
        Slider(
            value = strokeSize,
            onValueChange = { strokeSize = it },
            valueRange = 1f..maxStrokeSize,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Color selection
        Text(text = "Color:")
        Spacer(modifier = Modifier.height(8.dp))

        // Color grid
        val colors = listOf(
            listOf(Color.Black, Color.DarkGray, Color.Gray, Color.LightGray),
            listOf(Color.Red, Color.Blue, Color.Green, Color(0xFF8B4513)),
            listOf(Color(0xFFFF69B4), Color(0xFFFF8C00), Color(0xFF800080), Color(0xFF008080))
        )

        colors.forEach { colorRow ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorRow.forEach { color ->
                    ColorButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onSelect = { selectedColor = color }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pen profile info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Current Profile",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pen: ${currentPen.displayName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Stroke: ${strokeSize.toInt()}px",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Color: ${selectedColor}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Style: ${currentProfile.getOnyxStrokeStyle()}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean = false,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(4.dp)
            .background(color, CircleShape)
            .clip(CircleShape)
            .noRippleClickable(onClick = onSelect)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Black else Color.LightGray,
                shape = CircleShape
            )
    )
}