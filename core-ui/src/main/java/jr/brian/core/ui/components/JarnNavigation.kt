package jr.brian.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.focus.JARN_TAB_DPAD_SETTLE_MS
import jr.brian.core.ui.focus.JARN_TAB_HANDOFF_MS
import jr.brian.core.ui.focus.JarnTabSwitchSource
import jr.brian.core.ui.focus.JarnTabsFocus
import jr.brian.core.ui.focus.LocalJarnTabSwitchFocus
import jr.brian.core.ui.focus.jarnTabSwitchTakesFocus
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnScreenActive
import androidx.compose.runtime.rememberUpdatedState
import jr.brian.core.ui.theme.LocalJarnSwitchMark

private const val SELECTED_TAB_TINT_ALPHA = 0.14f

internal fun jarnShoulderTabIndex(current: Int, count: Int, delta: Int): Int =
    if (count <= 0) 0 else (current + delta).coerceIn(0, count - 1)

enum class JarnTabsStyle { Top, Rail }

/**
 * A row (or rail) of tabs. With [reorder], holding A or long-pressing a tab lifts it so the strip
 * can be reordered; the caller drives the move, save and cancel. While it is reordered, the
 * shoulders do nothing here.
 */
@Composable
fun JarnTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: JarnTabsStyle = JarnTabsStyle.Top,
    showsSwitchMark: Boolean = false,
    reorder: JarnTabReorder? = null,
) {
    val shoulderModifier = modifier.onKeyEvent { event ->
        val delta = when (event.key) {
            Key.ButtonL1 -> -1
            Key.ButtonR1 -> 1
            else -> return@onKeyEvent false
        }
        if (event.type == KeyEventType.KeyDown && reorder?.isEditing != true) {
            val next = jarnShoulderTabIndex(selectedIndex, labels.size, delta)
            if (next != selectedIndex) onSelect(next)
        }
        true
    }
    val requesters = remember(labels.size) { List(labels.size) { FocusRequester() } }
    val viewRequesters = remember(labels.size) { List(labels.size) { BringIntoViewRequester() } }
    LaunchedEffect(selectedIndex, viewRequesters) {
        runCatching { viewRequesters.getOrNull(selectedIndex)?.bringIntoView() }
    }
    val tabsFocus = LocalJarnTabsFocus.current
    val group = remember { Any() }
    var groupFocused by remember { mutableStateOf(false) }
    val focusPicked = remember { arrayOfNulls<String>(1) }
    val latestSelected by rememberUpdatedState(selectedIndex)
    JarnTabSwitchEffect(labels.getOrNull(selectedIndex), { groupFocused }, focusPicked, tabsFocus, group) {
        runCatching { requesters.getOrNull(latestSelected)?.requestFocus() }
    }
    DisposableEffect(tabsFocus, group) { onDispose { tabsFocus.update(group, false) } }
    val focusModifier = shoulderModifier
        .then(if (reorder != null) Modifier.jarnReorderKeys(reorder) else Modifier)
        .onFocusChanged {
            groupFocused = it.hasFocus
            tabsFocus.update(group, it.hasFocus)
        }
        .focusProperties { onEnter = { requesters.getOrNull(selectedIndex)?.requestFocus() } }
        .focusGroup()
    val strip = JarnTabStrip(labels, selectedIndex, requesters, viewRequesters, onSelect, focusPicked, reorder)
    val gap = Arrangement.spacedBy(JarnTheme.space.xs)
    val ringPadding = PaddingValues(JarnTheme.space.lg)
    when (style) {
        JarnTabsStyle.Top -> Row(
            modifier = focusModifier
                .selectableGroup()
                .horizontalScroll(rememberScrollState())
                .padding(ringPadding),
            horizontalArrangement = gap,
        ) { JarnTabItems(strip, Modifier, showsSwitchMark) }

        JarnTabsStyle.Rail -> Column(
            modifier = focusModifier
                .selectableGroup()
                .verticalScroll(rememberScrollState())
                .padding(ringPadding),
            verticalArrangement = gap,
        ) { JarnTabItems(strip, Modifier.fillMaxWidth(), showsSwitchMark) }
    }
}

/** Everything one strip's tabs share. [focusPicked] names the tab the strip's own focus picked. */
private class JarnTabStrip(
    val labels: List<String>,
    val selectedIndex: Int,
    val requesters: List<FocusRequester>,
    val viewRequesters: List<BringIntoViewRequester>,
    val onSelect: (Int) -> Unit,
    val focusPicked: Array<String?>,
    val reorder: JarnTabReorder?,
) {
    /** The caller's tab indices in the order they show, which is the working order while reordered. */
    val shown: List<Int>
        get() = reorder?.order?.takeIf { reorder.isEditing && it.size == labels.size } ?: labels.indices.toList()
}

/**
 * After each switch, keeps focus on the selected tab when the strip holds it, then hands focus to
 * the new tab's content when [LocalJarnTabSwitchFocus] allows that switch. A tab the strip's own
 * focus picked came from the D-pad and waits [JARN_TAB_DPAD_SETTLE_MS] first, so a newer step
 * cancels it. Any other switch came from a shoulder. The first selection never hands off, and
 * neither does a switch made while the controller is on the other screen. It follows the selected
 * tab by its label, so a reordered strip whose selected tab moved is not a switch.
 */
@Composable
private fun JarnTabSwitchEffect(
    selectedKey: String?,
    groupFocused: () -> Boolean,
    focusPicked: Array<String?>,
    tabsFocus: JarnTabsFocus,
    group: Any,
    focusSelectedTab: () -> Unit,
) {
    val rule = LocalJarnTabSwitchFocus.current
    val active by rememberUpdatedState(LocalJarnScreenActive.current)
    val shownFirst = remember { selectedKey }
    val switched = remember { booleanArrayOf(false) }
    val stripHadFocus = remember(selectedKey) { groupFocused() }
    LaunchedEffect(selectedKey) {
        tabsFocus.endHandOff(group)
        if (groupFocused()) focusSelectedTab()
        val source = if (focusPicked[0] == selectedKey) JarnTabSwitchSource.Dpad else JarnTabSwitchSource.Shoulder
        focusPicked[0] = null
        if (selectedKey != shownFirst) switched[0] = true
        if (!switched[0] || !active || !jarnTabSwitchTakesFocus(rule, source, stripHadFocus)) return@LaunchedEffect
        if (source == JarnTabSwitchSource.Dpad) delay(JARN_TAB_DPAD_SETTLE_MS)
        withFrameNanos { }
        tabsFocus.handOff(group)
        delay(JARN_TAB_HANDOFF_MS)
        tabsFocus.endHandOff(group)
    }
}

@Composable
private fun JarnTabItems(strip: JarnTabStrip, itemModifier: Modifier, showsSwitchMark: Boolean) {
    val gapPx = with(LocalDensity.current) { JarnTheme.space.xs.toPx() }
    strip.shown.forEach { index ->
        val label = strip.labels[index]
        val selected = index == strip.selectedIndex
        key(label) {
            JarnTab(
                label = label,
                selected = selected,
                requester = strip.requesters[index],
                onClick = { strip.onSelect(index) },
                onFocusPick = {
                    strip.focusPicked[0] = label
                    strip.onSelect(index)
                },
                modifier = itemModifier
                    .bringIntoViewRequester(strip.viewRequesters[index])
                    .jarnTabReorderable(strip, index, gapPx),
                showsSwitchMark = showsSwitchMark && selected,
                lifted = strip.reorder?.held == index,
            )
        }
    }
}

/** Hold A or long-press to lift the tab, and slide aside when another tab moves past. */
@Composable
private fun Modifier.jarnTabReorderable(strip: JarnTabStrip, index: Int, gapPx: Float): Modifier {
    val reorder = strip.reorder ?: return this
    val lifted = reorder.held == index
    return this
        .graphicsLayer { translationX = if (lifted) reorder.dragOffset else 0f }
        .jarnSlideToPlace(skip = lifted && reorder.dragging)
        .jarnTabWidth(reorder, index)
        .jarnTabDrag(reorder, index, { strip.labels.size }, gapPx)
        .jarnHoldA(onTap = { strip.onSelect(index) }, onHold = { reorder.begin(index, strip.labels.size) })
}

@Composable
private fun JarnTab(
    label: String,
    selected: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit,
    onFocusPick: () -> Unit,
    modifier: Modifier,
    showsSwitchMark: Boolean,
    lifted: Boolean = false,
) {
    val colors = JarnTheme.colors
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    val background by animateColorAsState(
        if (selected) colors.textHigh.copy(alpha = SELECTED_TAB_TINT_ALPHA) else colors.textHigh.copy(alpha = 0f),
        JarnTheme.motion.small.spec(),
    )
    Box(
        modifier = modifier
            .jarnTabLift(lifted, shape)
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused && !selected) onFocusPick() }
            .semantics { this.selected = selected }
            .jarnInteractive(shape, source, role = Role.Tab, onClick = onClick)
            .heightIn(min = 36.dp)
            .background(background, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        JarnText(
            text = label,
            modifier = Modifier.padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.xs),
            style = if (selected) JarnTheme.type.labelStrong else JarnTheme.type.label,
            color = if (selected) colors.textHigh else colors.textMid,
            maxLines = 1,
        )
        if (showsSwitchMark) JarnSwitchMarkLine()
    }
}

/** The held tab rises a little and gets the accent outline. */
@Composable
private fun Modifier.jarnTabLift(lifted: Boolean, shape: Shape): Modifier {
    val scale by animateFloatAsState(if (lifted) JARN_TAB_LIFT_SCALE else 1f, JarnTheme.motion.small.spec())
    val outline = if (lifted) Modifier.border(2.dp, JarnTheme.colors.accent, shape) else Modifier
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.then(outline)
}

/** Sized by its parent, so showing it never changes the parent's layout. */
@Composable
fun BoxScope.JarnSwitchMarkLine() {
    val mark = LocalJarnSwitchMark.current
    if (!mark.enabled) return
    val accent = JarnTheme.colors.accent
    Box(
        modifier = Modifier.matchParentSize().padding(horizontal = JarnTheme.space.md),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .graphicsLayer {
                    scaleX = mark.widthFraction
                    alpha = mark.alpha
                }
                .background(accent, CircleShape),
        )
    }
}

@Composable
fun JarnChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.radius.smShape
    val selectedState = stringResource(
        if (selected) R.string.jarn_state_selected else R.string.jarn_state_not_selected,
    )
    val interactive = if (onClick != null) {
        val source = remember { MutableInteractionSource() }
        Modifier
            .semantics { stateDescription = selectedState }
            .jarnInteractive(shape, source, role = Role.Checkbox, onClick = onClick)
    } else {
        Modifier
    }
    val outline = if (selected) Modifier.border(2.dp, colors.accent, shape) else Modifier
    Row(
        modifier = modifier
            .then(interactive)
            .heightIn(min = 30.dp)
            .background(colors.raised, shape)
            .then(outline)
            .padding(horizontal = 10.dp, vertical = JarnTheme.space.xs),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) JarnCheckGlyph(colors.accent, size = 14.dp)
        JarnText(
            text = label,
            style = JarnTheme.type.captionStrong,
            color = if (selected) colors.textHigh else colors.textMid,
            maxLines = 1,
        )
    }
}

enum class JarnKeyShape { Face, Shoulder, System, Dpad }

enum class JarnKey(@param:StringRes val labelRes: Int, val shape: JarnKeyShape) {
    A(R.string.jarn_key_a, JarnKeyShape.Face),
    B(R.string.jarn_key_b, JarnKeyShape.Face),
    X(R.string.jarn_key_x, JarnKeyShape.Face),
    Y(R.string.jarn_key_y, JarnKeyShape.Face),
    L1(R.string.jarn_key_l1, JarnKeyShape.Shoulder),
    R1(R.string.jarn_key_r1, JarnKeyShape.Shoulder),
    L2(R.string.jarn_key_l2, JarnKeyShape.Shoulder),
    R2(R.string.jarn_key_r2, JarnKeyShape.Shoulder),
    L1R1(R.string.jarn_key_l1_r1, JarnKeyShape.Shoulder),
    L2R2(R.string.jarn_key_l2_r2, JarnKeyShape.Shoulder),
    L3(R.string.jarn_key_l3, JarnKeyShape.Shoulder),
    R3(R.string.jarn_key_r3, JarnKeyShape.Shoulder),
    Start(R.string.jarn_key_start, JarnKeyShape.System),
    Select(R.string.jarn_key_select, JarnKeyShape.System),
    Dpad(R.string.jarn_key_dpad, JarnKeyShape.Dpad),
}

@Composable
private fun JarnKeyShape.toShape(): Shape =
    if (this == JarnKeyShape.Face) CircleShape else JarnTheme.radius.smShape

@Composable
fun JarnKeyCap(key: JarnKey, modifier: Modifier = Modifier, active: Boolean = true) {
    val colors = JarnTheme.colors
    val isFace = key.shape == JarnKeyShape.Face
    Box(
        modifier = modifier
            .then(if (isFace) Modifier.size(24.dp) else Modifier.heightIn(min = 24.dp).widthIn(min = 28.dp))
            .background(colors.raised, key.shape.toShape())
            .padding(horizontal = if (isFace) 0.dp else JarnTheme.space.xs),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(
            text = stringResource(key.labelRes),
            style = JarnTheme.type.captionStrong,
            color = if (active) colors.textHigh else colors.textLow,
            maxLines = 1,
        )
    }
}

@Immutable
data class JarnHint(val key: JarnKey, val label: String)

/**
 * [trailing] hints sit at the right end, with the same padding as the start. On a narrow screen
 * whole hints drop out, lowest priority first, so no label is ever cut.
 */
@Composable
fun JarnHintBar(
    hints: List<JarnHint>,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    trailing: List<JarnHint> = emptyList(),
) {
    JarnFittedHints(
        leading = hints,
        trailing = trailing,
        gap = JarnTheme.space.lg,
        modifier = modifier
            .fillMaxWidth()
            .height(JarnTheme.space.hintBarHeight)
            .background(JarnTheme.colors.surface)
            .padding(horizontal = JarnTheme.space.lg),
    ) { hint -> JarnHintItem(hint, active) }
}

@Composable
private fun JarnHintItem(hint: JarnHint, active: Boolean) {
    val colors = JarnTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnKeyCap(hint.key, active = active)
        JarnText(hint.label, JarnTheme.type.caption, if (active) colors.textMid else colors.textLow, maxLines = 1)
    }
}

enum class JarnStepState { Done, Current, Upcoming }

internal fun jarnStepState(index: Int, current: Int): JarnStepState = when {
    index < current -> JarnStepState.Done
    index == current -> JarnStepState.Current
    else -> JarnStepState.Upcoming
}

@Composable
fun JarnStepper(current: Int, total: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.jarn_stepper_progress, current + 1, total)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index -> JarnStepMark(jarnStepState(index, current)) }
    }
}

@Composable
private fun JarnStepMark(state: JarnStepState) {
    val colors = JarnTheme.colors
    val color = when (state) {
        JarnStepState.Done -> colors.textMid
        JarnStepState.Current -> colors.accent
        JarnStepState.Upcoming -> colors.line
    }
    val width = if (state == JarnStepState.Current) 32.dp else 16.dp
    Box(Modifier.size(width = width, height = 6.dp).background(color, CircleShape))
}
