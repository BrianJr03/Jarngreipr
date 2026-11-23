package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.scraper.JarnControllerKeyboard

private const val SWATCH_COLUMNS = 6
private val SwatchSelectedBorder = 3.dp

/** The same preset colors Classic offers, so a saved choice looks the same in both. */
private val FolderBackgroundColors: List<Color> = listOf(
    Color(0xFF000000),
    Color(0xFF1E1E2E),
    Color(0xFF0F3460),
    Color(0xFF1A3A6B),
    Color(0xFF1F4D2B),
    Color(0xFF3A1F4D),
    Color(0xFF4D1F1F),
    Color(0xFF4D3A1F),
    Color(0xFFE94560),
    Color(0xFFC97B12),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
)

@Composable
internal fun CanvasKeyboardSheet(
    label: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    initialText: String = "",
) {
    JarnSheet(visible = true, onDismiss = onCancel) {
        JarnControllerKeyboard(
            label = label,
            masked = false,
            onDone = onDone,
            onCancel = onCancel,
            modifier = Modifier.fillMaxWidth(),
            initialText = initialText,
        )
    }
}

/** Focus starts on Cancel, because deleting a folder cannot be undone. */
@Composable
internal fun CanvasFolderConfirmDialog(
    @StringRes messageRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_folder_delete_title),
            message = stringResource(messageRes),
            confirmLabel = stringResource(R.string.jarnfe_folder_delete),
            dismissLabel = stringResource(R.string.jarnfe_canvas_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}

@Composable
private fun FolderSheetColumn(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(scroll)
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) { content() }
    }
}

@Composable
internal fun CanvasDisplayChoiceSheet(onPick: (JarnAppDisplay) -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    CanvasModalSheet(stringResource(R.string.jarnfe_folder_which_display), first, onClose) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_folder_display_top),
            onClick = { onPick(JarnAppDisplay.Primary) },
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_folder_display_bottom),
            onClick = { onPick(JarnAppDisplay.Current) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun CanvasFolderBackgroundSheet(
    folder: JarnCanvasFolder,
    onColor: (Int?) -> Unit,
    onChooseImage: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val hasBackground = folder.backgroundColorArgb != null || folder.backgroundImagePath != null
    CanvasModalSheet(stringResource(R.string.jarnfe_folder_background_title), first, onClose) {
        FolderSheetColumn {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_folder_background_image),
                onClick = onChooseImage,
                modifier = Modifier.fillMaxWidth().focusRequester(first),
            )
            if (hasBackground) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_background_clear),
                    onClick = {
                        first.requestFocus()
                        onColor(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FolderBackgroundColors.chunked(SWATCH_COLUMNS).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
                ) {
                    row.forEachIndexed { index, color ->
                        FolderColorSwatch(
                            color = color,
                            number = rowIndex * SWATCH_COLUMNS + index + 1,
                            selected = folder.backgroundColorArgb == color.toArgb(),
                            onClick = { onColor(color.toArgb()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SWATCH_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FolderColorSwatch(
    color: Color,
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    val description = stringResource(
        if (selected) R.string.jarnfe_folder_background_color_selected else R.string.jarnfe_folder_background_color,
        number,
    )
    val outline = if (selected) {
        Modifier.border(SwatchSelectedBorder, JarnTheme.colors.textHigh, shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .jarnInteractive(shape, source, onClick = onClick)
            .background(color, shape)
            .then(outline),
    )
}
