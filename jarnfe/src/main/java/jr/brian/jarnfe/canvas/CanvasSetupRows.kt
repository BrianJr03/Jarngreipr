package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.settings.JarnWallpaperSection
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun CanvasSetupRows(
    data: JarnCanvasData,
    page: JarnCanvasPage,
    onTidy: () -> Unit,
    onAxisFocused: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val apply: (JarnCanvasSetupChange, () -> Unit) -> Unit = { change, onRefused ->
        scope.launch {
            val result = data.applyPageSetup(page.index, change)
            if (result is JarnCanvasSetupResult.Refused) {
                JarnNotices.show(result.reason.messageRes)
                onRefused()
            }
        }
    }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_canvas_option_tidy),
        supporting = stringResource(R.string.jarnfe_canvas_option_tidy_detail),
        onClick = onTidy,
        modifier = Modifier.fillMaxWidth(),
    )
    CanvasAxisRow(page.layout, onAxisFocused) { count, onRefused ->
        apply(JarnCanvasSetupChange.CrossAxis(page.layout.activeOrientation, count), onRefused)
    }
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_canvas_option_menu_tile),
        supporting = stringResource(R.string.jarnfe_canvas_option_menu_tile_detail),
        checked = page.layout.menuButtonVisible,
        onCheckedChange = { apply(JarnCanvasSetupChange.MenuTile(it)) {} },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CanvasAxisRow(
    layout: CanvasLayout,
    onFocused: (Boolean) -> Unit,
    onChange: (Int, onRefused: () -> Unit) -> Unit,
) {
    val orientation = layout.activeOrientation
    val saved = layout.activeCrossAxis
    var shown by remember(orientation, saved) { mutableIntStateOf(saved) }
    JarnSliderRow(
        label = stringResource(orientation.axisLabelRes),
        value = shown.toFloat(),
        valueText = shown.toString(),
        onValueChange = change@{ raw ->
            val target = raw.roundToInt()
            if (target == shown) return@change
            val result = canvasLayoutAfterSetup(layout, JarnCanvasSetupChange.CrossAxis(orientation, target))
            if (result is JarnCanvasSetupResult.Refused) {
                JarnNotices.show(result.reason.messageRes)
            } else {
                shown = target
                onChange(target) { shown = saved }
            }
        },
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocused(it.isFocused) },
        valueRange = CanvasLayout.MIN_AXIS.toFloat()..CanvasLayout.MAX_AXIS.toFloat(),
        steps = CanvasLayout.MAX_AXIS - CanvasLayout.MIN_AXIS,
    )
}

@Composable
internal fun CanvasWallpaperSheet(onBack: () -> Unit) {
    val first = remember { FocusRequester() }
    CanvasModalSheet(stringResource(R.string.jarnfe_wallpaper_title), first, onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            JarnWallpaperSection(showTitle = false, initialFocus = first)
        }
    }
}

@Composable
internal fun CanvasTidyDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_canvas_tidy_title),
            message = stringResource(R.string.jarnfe_canvas_tidy_message),
            confirmLabel = stringResource(R.string.jarnfe_canvas_option_tidy),
            dismissLabel = stringResource(R.string.jarnfe_canvas_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

private val CanvasScrollOrientation.axisLabelRes: Int
    @StringRes get() = when (this) {
        CanvasScrollOrientation.VERTICAL -> R.string.jarnfe_canvas_option_columns
        CanvasScrollOrientation.HORIZONTAL -> R.string.jarnfe_canvas_option_rows
    }

private val JarnCanvasSetupRefusal.messageRes: Int
    @StringRes get() = when (this) {
        JarnCanvasSetupRefusal.TilesPastEdge -> R.string.jarnfe_canvas_notice_tiles_past_edge
        JarnCanvasSetupRefusal.MenuCellTaken -> R.string.jarnfe_canvas_notice_menu_cell_taken
        JarnCanvasSetupRefusal.NotCanvas -> R.string.jarnfe_canvas_notice_setup_unavailable
    }
