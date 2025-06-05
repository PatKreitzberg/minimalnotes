package com.wyldsoft.notes.ui.gestures

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Wrapper composable that adds swipe-back gesture detection
 */
@Composable
fun SwipeBackGestureWrapper(
    onSwipeBack: () -> Unit,
    enabled: Boolean = true,
    threshold: Float = 100f, // minimum swipe distance in dp
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { threshold.dp.toPx() }

    var startX by remember { mutableStateOf(0f) }
    var startY by remember { mutableStateOf(0f) }
    var isSwipeInProgress by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            startY = offset.y
                            isSwipeInProgress = true
                        },
                        onDragEnd = {
                            isSwipeInProgress = false
                        },
                        onDrag = { change, _ ->
                            if (isSwipeInProgress) {
                                val deltaX = change.position.x - startX
                                val deltaY = change.position.y - startY

                                // Check if this is a right swipe (swipe back)
                                if (deltaX > thresholdPx && abs(deltaY) < thresholdPx / 2) {
                                    // Only trigger if swipe started from left edge (first 20% of screen)
                                    if (startX < size.width * 0.2f) {
                                        onSwipeBack()
                                        isSwipeInProgress = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        content()
    }
}