package jr.brian.jarnfe.tools

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/** Moves a system cursor with touch or a pad. Commands stay in order on one background worker. */
@Composable
internal fun JarnTrackpadPanel(
    tools: JarnInputTools,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status by tools.pointerStatus.collectAsState(JarnPointerStatus())
    val isReady by rememberUpdatedState(status.ready)
    val commands = remember { Channel<JarnPointerCommand>(Channel.UNLIMITED) }
    val metrics = LocalContext.current.resources.displayMetrics
    val controller = remember(tools) {
        JarnTrackpadController(JarnTrackpadProcessor(metrics.xdpi, metrics.ydpi), tools.pointerBounds()) {
            if (isReady) commands.trySend(it)
        }
    }
    LaunchedEffect(tools) { tools.connectPointer() }
    LaunchedEffect(tools) {
        withContext(Dispatchers.IO) { for (command in commands) tools.sendPointer(command) }
    }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = { trackpadBindings(status, controller, tools, onClose) },
    ) {
        Column(
            modifier = modifier.fillMaxSize().padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            JarnText(
                text = stringResource(status.messageRes()),
                style = JarnTheme.type.label,
                color = if (status.ready) JarnTheme.colors.textMid else JarnTheme.colors.textHigh,
                modifier = Modifier.fillMaxWidth(),
            )
            TrackpadSurface(controller, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

private fun JarnBindingsBuilder.trackpadBindings(
    status: JarnPointerStatus,
    controller: JarnTrackpadController,
    tools: JarnInputTools,
    onClose: () -> Unit,
) {
    on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
    if (!status.ready) {
        if (status.available && !status.permissionGranted) {
            on(JarnAction.Search, R.string.jarnfe_tools_grant, onPress = tools::requestPointerPermission)
        }
        return
    }
    on(JarnAction.Confirm, R.string.jarnfe_tools_hint_click) { controller.click(secondary = false) }
    on(JarnAction.Context, R.string.jarnfe_tools_hint_right_click) { controller.click(secondary = true) }
    on(JarnAction.TabPrevious, R.string.jarnfe_tools_hint_scroll_up, repeats = true, onPress = controller::scrollUp)
    on(JarnAction.TabNext, R.string.jarnfe_tools_hint_scroll_down, repeats = true, onPress = controller::scrollDown)
    on(JarnAction.MoveUp, R.string.jarnfe_hint_move) { controller.step(JarnPadDirection.Up) }
    on(JarnAction.MoveDown) { controller.step(JarnPadDirection.Down) }
    on(JarnAction.MoveLeft) { controller.step(JarnPadDirection.Left) }
    on(JarnAction.MoveRight) { controller.step(JarnPadDirection.Right) }
}

@StringRes
private fun JarnPointerStatus.messageRes(): Int = when {
    !available -> R.string.jarnfe_tools_pointer_missing
    !permissionGranted -> R.string.jarnfe_tools_pointer_permission
    !connected -> R.string.jarnfe_tools_pointer_connecting
    else -> R.string.jarnfe_tools_pointer_ready
}

@Composable
private fun TrackpadSurface(controller: JarnTrackpadController, modifier: Modifier = Modifier) {
    val shape = JarnTheme.radius.mdShape
    Box(
        modifier = modifier
            .background(JarnTheme.colors.raised, shape)
            .pointerInput(controller) { readTrackpadTouches(controller) },
        contentAlignment = Alignment.Center,
    ) {
        JarnText(
            text = stringResource(R.string.jarnfe_tools_surface_hint),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(JarnTheme.space.lg),
        )
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.readTrackpadTouches(
    controller: JarnTrackpadController,
) {
    val gesture = JarnTrackpadGesture(viewConfiguration.touchSlop)
    awaitPointerEventScope {
        while (true) {
            val changes = awaitPointerEvent(PointerEventPass.Main).changes
            val events = gesture.onFrame(changes.map(PointerInputChange::toTouchPoint))
            changes.forEach(PointerInputChange::consume)
            events.forEach { event ->
                when (event) {
                    is JarnTrackpadGestureEvent.Drag -> controller.drag(event.dx, event.dy, event.timestampNs)
                    is JarnTrackpadGestureEvent.Scroll -> controller.scrollBy(event.dyPx / size.height)
                    JarnTrackpadGestureEvent.Tap -> controller.click(secondary = false)
                    JarnTrackpadGestureEvent.TwoFingerTap -> controller.click(secondary = true)
                    JarnTrackpadGestureEvent.End -> controller.endDrag()
                }
            }
        }
    }
}

private fun PointerInputChange.toTouchPoint() = JarnTouchPoint(
    id = id.value,
    pressed = pressed,
    current = JarnTouchSample(position.x, position.y, uptimeMillis),
    history = historical.map { JarnTouchSample(it.position.x, it.position.y, it.uptimeMillis) },
)
