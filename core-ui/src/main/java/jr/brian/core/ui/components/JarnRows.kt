package jr.brian.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.focus.jarnDisabledAlpha
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import kotlin.math.roundToInt

internal fun jarnSliderStep(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, direction: Int): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f || steps <= 0) return value.coerceIn(range)
    val increment = span / steps
    val index = ((value.coerceIn(range) - range.start) / increment).roundToInt() + direction
    return (range.start + index.coerceIn(0, steps) * increment).coerceIn(range)
}

internal fun jarnSliderValueAt(fraction: Float, range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    val span = range.endInclusive - range.start
    val clamped = fraction.coerceIn(0f, 1f)
    if (span <= 0f) return range.start
    if (steps <= 0) return range.start + span * clamped
    return (range.start + (clamped * steps).roundToInt() * (span / steps)).coerceIn(range)
}

@Composable
private fun JarnRowLabels(label: String, supporting: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        JarnText(label, JarnTheme.type.label, JarnTheme.colors.textHigh)
        supporting?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid) }
    }
}

@Composable
private fun JarnRowFrame(
    onClick: () -> Unit,
    enabled: Boolean,
    focusableWhenDisabled: Boolean,
    role: Role,
    modifier: Modifier,
    interactionSource: MutableInteractionSource?,
    content: @Composable RowScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    Row(
        modifier = modifier
            .jarnInteractive(shape, source, enabled, focusableWhenDisabled, role, onClick)
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(JarnTheme.colors.surface, shape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun JarnNavigateRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    JarnRowFrame(onClick, enabled, focusableWhenDisabled, Role.Button, modifier, interactionSource) {
        JarnRowLabels(label, supporting, Modifier.weight(1f))
        JarnChevronGlyph(JarnTheme.colors.textMid)
    }
}

@Composable
fun JarnValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    JarnRowFrame(onClick, enabled, focusableWhenDisabled, Role.Button, modifier, interactionSource) {
        JarnRowLabels(label, supporting, Modifier.weight(1f))
        JarnText(value, JarnTheme.type.label, JarnTheme.colors.textMid, maxLines = 1)
        JarnChevronGlyph(JarnTheme.colors.textMid)
    }
}

@Composable
fun JarnToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val state = stringResource(if (checked) R.string.jarn_state_on else R.string.jarn_state_off)
    JarnRowFrame(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        focusableWhenDisabled = focusableWhenDisabled,
        role = Role.Switch,
        modifier = modifier.semantics { stateDescription = state },
        interactionSource = interactionSource,
    ) {
        JarnRowLabels(label, supporting, Modifier.weight(1f))
        JarnSwitch(checked)
    }
}

@Composable
fun JarnSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = JarnTheme.colors
    val motion = JarnTheme.motion
    val track by animateColorAsState(if (checked) colors.accent else colors.raised, motion.small.spec())
    val thumb by animateColorAsState(if (checked) colors.onAccent else colors.textLow, motion.small.spec())
    val thumbOffset by animateDpAsState(if (checked) 20.dp else 4.dp, motion.small.spec())
    val outline = if (checked) colors.accent else colors.textLow
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .background(track, CircleShape)
            .border(2.dp, outline, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .background(thumb, CircleShape),
        )
    }
}

@Composable
fun JarnSliderRow(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 20,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    val latestValue by rememberUpdatedState(value)
    val latestChange by rememberUpdatedState(onValueChange)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        modifier = modifier
            .jarnFocus(shape, source, enabled)
            .focusProperties { canFocus = enabled || focusableWhenDisabled }
            .onKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionLeft -> if (rtl) 1 else -1
                    Key.DirectionRight -> if (rtl) -1 else 1
                    else -> return@onKeyEvent false
                }
                if (enabled && event.type == KeyEventType.KeyDown) {
                    val next = jarnSliderStep(latestValue, valueRange, steps, direction)
                    if (next != latestValue) latestChange(next)
                }
                true
            }
            .semantics {
                if (!enabled) disabled()
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, (steps - 1).coerceAtLeast(0))
                if (enabled) {
                    setProgress { target ->
                        latestChange(target.coerceIn(valueRange))
                        true
                    }
                }
            }
            .focusable(enabled || focusableWhenDisabled, source)
            .jarnDisabledAlpha(enabled)
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(JarnTheme.colors.surface, shape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarnRowLabels(label, null, Modifier.weight(1f))
            JarnText(valueText, JarnTheme.type.labelStrong, JarnTheme.colors.textMid, maxLines = 1)
        }
        JarnSliderTrack(value, valueRange, steps, enabled, rtl) { latestChange(it) }
    }
}

@Composable
private fun JarnSliderTrack(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    rtl: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val colors = JarnTheme.colors
    val span = range.endInclusive - range.start
    val fraction = if (span > 0f) ((value - range.start) / span).coerceIn(0f, 1f) else 0f
    val latestChange by rememberUpdatedState(onValueChange)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(enabled, range, steps, rtl) {
                if (!enabled) return@pointerInput
                fun emit(x: Float) {
                    val raw = (x / size.width).coerceIn(0f, 1f)
                    latestChange(jarnSliderValueAt(if (rtl) 1f - raw else raw, range, steps))
                }
                detectTapGestures { emit(it.x) }
            }
            .pointerInput(enabled, range, steps, rtl) {
                if (!enabled) return@pointerInput
                fun emit(x: Float) {
                    val raw = (x / size.width).coerceIn(0f, 1f)
                    latestChange(jarnSliderValueAt(if (rtl) 1f - raw else raw, range, steps))
                }
                detectHorizontalDragGestures(onDragStart = { emit(it.x) }) { change, _ ->
                    change.consume()
                    emit(change.position.x)
                }
            },
    ) {
        val y = size.height / 2f
        val inset = 8.dp.toPx()
        val usable = (size.width - inset * 2f).coerceAtLeast(0f)
        val startX = if (rtl) size.width - inset else inset
        val thumbX = if (rtl) startX - usable * fraction else startX + usable * fraction
        val endX = if (rtl) inset else size.width - inset
        val stroke = 4.dp.toPx()
        drawLine(colors.textLow, Offset(startX, y), Offset(endX, y), stroke, StrokeCap.Round)
        drawLine(colors.accent, Offset(startX, y), Offset(thumbX, y), stroke, StrokeCap.Round)
        drawCircle(colors.accent, radius = 8.dp.toPx(), center = Offset(thumbX, y))
    }
}
