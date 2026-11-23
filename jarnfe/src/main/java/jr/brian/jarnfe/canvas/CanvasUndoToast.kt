package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.rememberJarnDisplay

private val UndoToastMaxWidth = 420.dp
private val UndoTimerHeight = 2.dp

/** Ends the undo window when the app stops or Canvas leaves the screen. */
@Composable
internal fun CanvasUndoExits(undo: CanvasUndoController) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, undo) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) undo.settle()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            undo.settle()
        }
    }
}

/** The button is never a focus stop. A controller undoes with Y, shown in the hint bar. */
@Composable
internal fun CanvasUndoToast(entry: CanvasUndoEntry, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    JarnSurface(modifier = modifier.widthIn(max = UndoToastMaxWidth)) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.sm),
                horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JarnText(
                    text = stringResource(entry.kind.messageRes),
                    style = JarnTheme.type.body,
                    color = JarnTheme.colors.textHigh,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                JarnButton(
                    label = stringResource(R.string.jarnfe_canvas_undo),
                    onClick = onUndo,
                    modifier = Modifier.focusProperties { canFocus = false },
                    variant = JarnButtonVariant.Secondary,
                )
            }
            if (rememberJarnDisplay().undoCountdownVisible) CanvasUndoTimer(entry)
        }
    }
}

@Composable
private fun CanvasUndoTimer(entry: CanvasUndoEntry) {
    if (JarnTheme.motion.reduced) return
    val remaining = remember(entry) { Animatable(1f) }
    LaunchedEffect(entry) {
        remaining.animateTo(0f, tween(CANVAS_UNDO_WINDOW_MS.toInt(), easing = LinearEasing))
    }
    val accent = JarnTheme.colors.accent
    Box(
        Modifier
            .fillMaxWidth()
            .height(UndoTimerHeight)
            .graphicsLayer {
                scaleX = remaining.value
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .background(accent),
    )
}

private val CanvasUndoKind.messageRes: Int
    get() = when (this) {
        CanvasUndoKind.Added -> R.string.jarnfe_canvas_undo_added
        CanvasUndoKind.Moved -> R.string.jarnfe_canvas_undo_moved
        CanvasUndoKind.Resized -> R.string.jarnfe_canvas_undo_resized
        CanvasUndoKind.Removed -> R.string.jarnfe_canvas_undo_removed
        CanvasUndoKind.MovedToFolder -> R.string.jarnfe_canvas_undo_moved_to_folder
        CanvasUndoKind.Tidied -> R.string.jarnfe_canvas_undo_tidied
    }
