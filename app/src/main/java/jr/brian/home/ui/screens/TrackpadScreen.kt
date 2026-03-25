package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.MotionEvent
import jr.brian.home.ui.components.controlpad.ShizukuStatusIndicator
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.TrackpadInputProcessor
import jr.brian.home.util.shizuku.ShizukuInputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun TrackpadScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isShizukuAvailable by ShizukuInputManager.isShizukuAvailable.collectAsStateWithLifecycle()
    val isShizukuPermissionGranted by ShizukuInputManager.isShizukuPermissionGranted.collectAsStateWithLifecycle()
    val isServiceConnected by ShizukuInputManager.isServiceConnected.collectAsStateWithLifecycle()

    val metrics = context.resources.displayMetrics
    val screenW = metrics.widthPixels.toFloat()
    val screenH = metrics.heightPixels.toFloat()

    // Absolute cursor position in screen pixels
    var cursorX by remember { mutableFloatStateOf(screenW / 2f) }
    var cursorY by remember { mutableFloatStateOf(screenH / 2f) }

    val processor = remember {
        TrackpadInputProcessor(metrics, metrics)
    }

    DisposableEffect(Unit) {
        ShizukuInputManager.initialize()
        onDispose { }
    }

    BackHandler { onDismiss() }

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            ScreenHeader(onBackClick = onDismiss)

            ShizukuStatusIndicator(
                isShizukuAvailable = isShizukuAvailable,
                isPermissionGranted = isShizukuPermissionGranted,
                isServiceConnected = isServiceConnected,
                onHelpClick = { ShizukuInputManager.requestPermission() }
            )

            TrackpadSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onDrag = { rawDxPx, rawDyPx, timestampNs ->
                    coroutineScope.launch(Dispatchers.IO) {
                        val delta = processor.process(rawDxPx, rawDyPx, timestampNs)
                        cursorX = (cursorX + delta.dx * screenW).coerceIn(0f, screenW)
                        cursorY = (cursorY + delta.dy * screenH).coerceIn(0f, screenH)
                        ShizukuInputManager.injectMouseMove(cursorX, cursorY)
                    }
                },
                onDragEnd = { processor.reset() },
                onTap = {
                    coroutineScope.launch(Dispatchers.IO) {
                        ShizukuInputManager.injectMouseClick(
                            cursorX, cursorY,
                            MotionEvent.BUTTON_PRIMARY,
                            MotionEvent.ACTION_DOWN
                        )
                        ShizukuInputManager.injectMouseClick(
                            cursorX, cursorY,
                            MotionEvent.BUTTON_PRIMARY,
                            MotionEvent.ACTION_UP
                        )
                    }
                },
                onTwoFingerTap = {
                    coroutineScope.launch(Dispatchers.IO) {
                        ShizukuInputManager.injectMouseClick(
                            cursorX, cursorY,
                            MotionEvent.BUTTON_SECONDARY,
                            MotionEvent.ACTION_DOWN
                        )
                        ShizukuInputManager.injectMouseClick(
                            cursorX, cursorY,
                            MotionEvent.BUTTON_SECONDARY,
                            MotionEvent.ACTION_UP
                        )
                    }
                },
                onTwoFingerScroll = { dy ->
                    coroutineScope.launch(Dispatchers.IO) {
                        ShizukuInputManager.injectMouseScroll(cursorX, cursorY, -dy * 10f)
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackpadSurface(
    onDrag: (rawDxPx: Float, rawDyPx: Float, timestampNs: Long) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    onTwoFingerTap: () -> Unit,
    onTwoFingerScroll: (dy: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val lastPositions = mutableMapOf<PointerId, Offset>()
                    val downPositions = mutableMapOf<PointerId, Offset>()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val changes = event.changes
                        val activeChanges = changes.filter { it.pressed }

                        // Two-finger gestures
                        if (activeChanges.size >= 2) {
                            val avgDy = activeChanges
                                .mapNotNull { c -> lastPositions[c.id]?.let { c.position.y - it.y } }
                                .average().toFloat()

                            if (abs(avgDy) > 0.5f) {
                                onTwoFingerScroll(avgDy / size.height)
                            }
                            activeChanges.forEach { c ->
                                lastPositions[c.id] = c.position
                                c.consume()
                            }
                            continue
                        }

                        // Check for two-finger tap release
                        val released = changes.filter { !it.pressed && lastPositions.containsKey(it.id) }
                        if (released.size >= 2) {
                            val allShortTravel = released.all { c ->
                                val down = downPositions[c.id] ?: c.position
                                val dx = c.position.x - down.x
                                val dy = c.position.y - down.y
                                sqrt(dx * dx + dy * dy) < 20f
                            }
                            if (allShortTravel) onTwoFingerTap()
                            onDragEnd()
                            lastPositions.clear()
                            downPositions.clear()
                            continue
                        }

                        val primary = changes.firstOrNull { it.pressed } ?: run {
                            val rel = changes.firstOrNull { !it.pressed && lastPositions.containsKey(it.id) }
                            if (rel != null) {
                                val down = downPositions[rel.id]
                                if (down != null) {
                                    val dx = rel.position.x - down.x
                                    val dy = rel.position.y - down.y
                                    if (sqrt(dx * dx + dy * dy) < 20f) onTap()
                                }
                                onDragEnd()
                                lastPositions.clear()
                                downPositions.clear()
                            }
                            continue
                        }

                        // Record initial press position for tap detection
                        if (!downPositions.containsKey(primary.id)) {
                            downPositions[primary.id] = primary.position
                        }

                        val lastPos = lastPositions[primary.id]
                        if (lastPos != null) {
                            // Drain historical samples first
                            primary.historical.forEach { h ->
                                val prev = lastPositions[primary.id] ?: h.position
                                onDrag(
                                    h.position.x - prev.x,
                                    h.position.y - prev.y,
                                    h.uptimeMillis * 1_000_000L
                                )
                                lastPositions[primary.id] = h.position
                            }
                            val cur = lastPositions[primary.id] ?: primary.position
                            onDrag(
                                primary.position.x - cur.x,
                                primary.position.y - cur.y,
                                primary.uptimeMillis * 1_000_000L
                            )
                        }

                        lastPositions[primary.id] = primary.position
                        primary.consume()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(40.dp)
                )
                Text(
                    text = "Slide to move cursor\nTap to click · Two fingers to right-click or scroll",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
