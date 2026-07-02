package jr.brian.home.esde.ui.frontend

import android.view.KeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val GAMEPAD_SCROLL_STEP: Dp = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemFilterSheet(
    systems: List<String>,
    hiddenSystems: Set<String>,
    onToggle: (systemName: String, hidden: Boolean) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OledCardColor
    ) {
        SystemFilterBody(
            systems = systems,
            hiddenSystems = hiddenSystems,
            onToggle = onToggle,
            onShowAll = onShowAll,
            onHideAll = onHideAll,
            onClose = onDismiss
        )
    }
}

@Composable
private fun SystemFilterBody(
    systems: List<String>,
    hiddenSystems: Set<String>,
    onToggle: (systemName: String, hidden: Boolean) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onClose: () -> Unit
) {
    val firstRowFocus = remember { FocusRequester() }
    LaunchedEffect(systems) {
        runCatching { firstRowFocus.requestFocus() }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { GAMEPAD_SCROLL_STEP.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .onPreviewKeyEvent { event ->
                handleGamepadScroll(event, scrollState, stepPx, scope)
            }
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SheetTitle()
        BulkActionRow(
            onShowAll = onShowAll,
            onHideAll = onHideAll
        )
        SystemToggleList(
            systems = systems,
            hiddenSystems = hiddenSystems,
            firstRowFocus = firstRowFocus,
            onToggle = onToggle
        )
        SheetCloseButton(onClick = onClose)
    }
}

@Composable
private fun SheetTitle() {
    Text(
        text = stringResource(R.string.system_filter_title),
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun BulkActionRow(
    onShowAll: () -> Unit,
    onHideAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BulkActionButton(
            label = stringResource(R.string.system_filter_show_all),
            onClick = onShowAll,
            modifier = Modifier.weight(1f)
        )
        BulkActionButton(
            label = stringResource(R.string.system_filter_hide_all),
            onClick = onHideAll,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BulkActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickWithHaptic(haptic) { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SystemToggleList(
    systems: List<String>,
    hiddenSystems: Set<String>,
    firstRowFocus: FocusRequester,
    onToggle: (systemName: String, hidden: Boolean) -> Unit
) {
    if (systems.isEmpty()) {
        Text(
            text = stringResource(R.string.system_filter_empty),
            color = Color.White.copy(alpha = 0.6f)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        systems.forEachIndexed { index, name ->
            val shown = name !in hiddenSystems
            val rowModifier = if (index == 0) {
                Modifier.focusRequester(firstRowFocus)
            } else {
                Modifier
            }
            Column(modifier = rowModifier) {
                ToggleSetting(
                    title = name,
                    description = stringResource(
                        if (shown) R.string.system_filter_row_shown
                        else R.string.system_filter_row_hidden
                    ),
                    checked = shown,
                    onCheckedChange = { checked -> onToggle(name, !checked) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SheetCloseButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.system_filter_close),
            color = ThemeAccentColor
        )
    }
}

private fun handleGamepadScroll(
    event: androidx.compose.ui.input.key.KeyEvent,
    scrollState: ScrollState,
    stepPx: Float,
    scope: CoroutineScope
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (scrollState.value < scrollState.maxValue) {
                scope.launch { scrollState.animateScrollBy(stepPx) }
            }
            false
        }
        KeyEvent.KEYCODE_DPAD_UP -> {
            if (scrollState.value > 0) {
                scope.launch { scrollState.animateScrollBy(-stepPx) }
            }
            false
        }
        else -> false
    }
}

private suspend fun ScrollState.animateScrollBy(delta: Float) {
    val target = (value + delta).toInt().coerceIn(0, maxValue)
    animateScrollTo(target)
}
