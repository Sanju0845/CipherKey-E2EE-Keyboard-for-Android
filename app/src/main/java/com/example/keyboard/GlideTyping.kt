package com.example.keyboard

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Tracks key hit areas for tap + glide (swipe) typing on a single row.
 */
class GlideKeyTracker {
    private val boundsByKey = mutableStateMapOf<String, Rect>()

    fun updateBounds(key: String, rect: Rect) {
        if (rect.width > 0f && rect.height > 0f) {
            boundsByKey[key] = rect
        }
    }

    fun keyAt(x: Float, y: Float): String? {
        for ((key, rect) in boundsByKey) {
            if (rect.contains(androidx.compose.ui.geometry.Offset(x, y))) {
                return key
            }
        }
        return null
    }

    fun buildGlideWord(path: List<String>): String {
        if (path.isEmpty()) return ""
        val sb = StringBuilder()
        var last: String? = null
        for (key in path) {
            if (key != last) {
                sb.append(key)
                last = key
            }
        }
        return sb.toString()
    }
}

@Composable
fun Modifier.trackGlideKey(key: String, tracker: GlideKeyTracker): Modifier =
    onGloballyPositioned { coordinates ->
        tracker.updateBounds(key, coordinates.boundsInParent())
    }

@Composable
fun Modifier.glideRowGestures(
    tracker: GlideKeyTracker,
    enabled: Boolean,
    onGlideWord: (String) -> Unit,
    onHighlightKey: (String?) -> Unit = {}
): Modifier {
    if (!enabled) return this

    return this.pointerInput(tracker) {
        val glidePath = mutableListOf<String>()

        fun appendKey(key: String) {
            if (glidePath.isEmpty() || glidePath.last() != key) {
                glidePath.add(key)
            }
            onHighlightKey(key)
        }

        fun resetGlide() {
            glidePath.clear()
            onHighlightKey(null)
        }

        detectDragGestures(
            onDragStart = { offset ->
                resetGlide()
                tracker.keyAt(offset.x, offset.y)?.let(::appendKey)
            },
            onDrag = { change, _ ->
                change.consume()
                tracker.keyAt(change.position.x, change.position.y)?.let(::appendKey)
            },
            onDragEnd = {
                val word = tracker.buildGlideWord(glidePath)
                if (word.isNotEmpty()) {
                    onGlideWord(word)
                }
                resetGlide()
            },
            onDragCancel = { resetGlide() }
        )
    }
}
