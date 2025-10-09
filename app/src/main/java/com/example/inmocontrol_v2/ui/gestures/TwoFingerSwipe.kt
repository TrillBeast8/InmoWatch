package com.example.inmocontrol_v2.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.detectTwoFingerSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown()
        var swipeTriggered = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                drag(event.changes.first().id) { change ->
                    if (!swipeTriggered) {
                        val horizontalDrag = change.position.x - change.previousPosition.x
                        if (abs(horizontalDrag) > 100f) {
                            if (horizontalDrag > 0) {
                                onSwipeRight()
                            } else {
                                onSwipeLeft()
                            }
                            swipeTriggered = true
                        }
                    }
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
