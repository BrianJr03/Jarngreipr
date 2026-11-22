package jr.brian.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnTheme

private class JarnLookProvider : PreviewParameterProvider<JarnLook> {
    override val values: Sequence<JarnLook> = JarnLooks.All.asSequence()
}

@Composable
private fun JarnPreviewFrame(look: JarnLook, content: @Composable ColumnScope.() -> Unit) {
    JarnTheme(look = look) {
        Column(
            modifier = Modifier
                .background(JarnTheme.colors.bg)
                .padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
            content = content,
        )
    }
}

@Preview(widthDp = 538)
@Composable
private fun JarnButtonsPreview(@PreviewParameter(JarnLookProvider::class) look: JarnLook) {
    JarnPreviewFrame(look) {
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnButton(stringResource(R.string.jarn_preview_play), {})
            JarnButton(stringResource(R.string.jarn_preview_options), {}, variant = JarnButtonVariant.Secondary)
            JarnButton(stringResource(R.string.jarn_preview_delete), {}, variant = JarnButtonVariant.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnButton(stringResource(R.string.jarn_preview_play), {}, loading = true)
            JarnButton(stringResource(R.string.jarn_preview_options), {}, enabled = false)
            JarnActionTile(stringResource(R.string.jarn_preview_canvas), ColorPainter(Color.White), {})
        }
    }
}

@Preview(widthDp = 538)
@Composable
private fun JarnRowsPreview(@PreviewParameter(JarnLookProvider::class) look: JarnLook) {
    JarnPreviewFrame(look) {
        JarnNavigateRow(stringResource(R.string.jarn_preview_settings), {})
        JarnValueRow(stringResource(R.string.jarn_preview_library), stringResource(R.string.jarn_preview_canvas), {})
        JarnToggleRow(stringResource(R.string.jarn_preview_hints), checked = true, onCheckedChange = {})
        JarnToggleRow(stringResource(R.string.jarn_preview_hints), checked = false, onCheckedChange = {})
        JarnSliderRow(
            label = stringResource(R.string.jarn_preview_brightness),
            value = 0.7f,
            valueText = stringResource(R.string.jarn_preview_percent),
            onValueChange = {},
        )
    }
}

@Preview(widthDp = 538)
@Composable
private fun JarnNavigationPreview(@PreviewParameter(JarnLookProvider::class) look: JarnLook) {
    JarnPreviewFrame(look) {
        JarnTabs(
            labels = listOf(
                stringResource(R.string.jarn_preview_library),
                stringResource(R.string.jarn_preview_favorites),
                stringResource(R.string.jarn_preview_settings),
            ),
            selectedIndex = 0,
            onSelect = {},
        )
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
            JarnChip(stringResource(R.string.jarn_preview_library))
            JarnChip(stringResource(R.string.jarn_preview_favorites), selected = true, onClick = {})
        }
        JarnStepper(current = 2, total = 5)
        JarnHintBar(
            listOf(
                JarnHint(JarnKey.A, stringResource(R.string.jarn_preview_play)),
                JarnHint(JarnKey.B, stringResource(R.string.jarn_preview_back)),
                JarnHint(JarnKey.X, stringResource(R.string.jarn_preview_options)),
                JarnHint(JarnKey.L1, stringResource(R.string.jarn_preview_library)),
            ),
        )
    }
}

@Preview(widthDp = 538)
@Composable
private fun JarnLookCardsPreview() {
    JarnPreviewFrame(JarnLooks.Default) {
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg)) {
            JarnLooks.All.forEach { look ->
                JarnLookCard(look, look == JarnLooks.Default, {}, Modifier.width(112.dp))
            }
        }
    }
}

@Preview(widthDp = 538, heightDp = 468)
@Composable
private fun JarnEmptyStatePreview() {
    JarnTheme {
        JarnScreenBackground {
            JarnEmptyState(
                title = stringResource(R.string.jarn_preview_empty_title),
                message = stringResource(R.string.jarn_preview_empty_body),
                actionLabel = stringResource(R.string.jarn_preview_empty_action),
                onAction = {},
            )
        }
    }
}

@Preview(widthDp = 538, heightDp = 468)
@Composable
private fun JarnDialogPreview() {
    JarnTheme {
        Box(Modifier.fillMaxSize().background(JarnTheme.colors.bg)) {
            JarnDialog(
                visible = true,
                title = stringResource(R.string.jarn_preview_dialog_title),
                message = stringResource(R.string.jarn_preview_dialog_body),
                confirmLabel = stringResource(R.string.jarn_preview_delete),
                dismissLabel = stringResource(R.string.jarn_preview_cancel),
                onConfirm = {},
                onDismiss = {},
                destructive = true,
            )
        }
    }
}

@Preview(widthDp = 538, heightDp = 468)
@Composable
private fun JarnSheetPreview() {
    JarnTheme {
        Box(Modifier.fillMaxSize().background(JarnTheme.colors.bg)) {
            JarnSheet(visible = true, onDismiss = {}, title = stringResource(R.string.jarn_preview_options)) {
                JarnNavigateRow(stringResource(R.string.jarn_preview_settings), {})
                Box(Modifier.height(JarnTheme.space.sm))
            }
        }
    }
}
