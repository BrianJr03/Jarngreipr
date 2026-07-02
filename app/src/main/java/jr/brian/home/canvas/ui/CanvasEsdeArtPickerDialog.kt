package jr.brian.home.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Single chooser for the ES-DE Display tile's [GameImageType] — used both
 * when adding a new tile and when re-typing an existing one. Reuses
 * [GameImageTypeSelector] (already chip-styled and focusable) so the picker
 * surface is consistent with ES-DE settings.
 *
 * Confirm always fires [onConfirm] then [onDismiss], so the dialog always
 * closes on a successful selection.
 */
@Composable
fun CanvasEsdeArtChooserDialog(
    initialType: GameImageType,
    initialContentScale: EsdeContentScale,
    titleRes: Int,
    onConfirm: (GameImageType, EsdeContentScale) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(initialType) { mutableStateOf(initialType) }
    var selectedScale by remember(initialContentScale) { mutableStateOf(initialContentScale) }

    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            color = OledCardColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    brush = borderBrush(isFocused = true),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(titleRes),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                GameImageTypeSelector(
                    selectedType = selected,
                    onTypeSelected = { selected = it }
                )

                EsdeContentScaleSelector(
                    selected = selectedScale,
                    onSelected = { selectedScale = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    HeaderActionButton(
                        label = stringResource(R.string.canvas_picker_cancel),
                        isPrimary = false,
                        enabled = true,
                        onClick = onDismiss
                    )
                    HeaderActionButton(
                        label = stringResource(R.string.canvas_esde_picker_confirm),
                        isPrimary = true,
                        enabled = selected.folderName != null,
                        onClick = {
                            onConfirm(selected, selectedScale)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Two-chip Fit/Crop toggle, styled to match `GameImageTypeChip` so the chooser
 * dialog reads as one coherent row of options.
 */
@Composable
private fun EsdeContentScaleSelector(
    selected: EsdeContentScale,
    onSelected: (EsdeContentScale) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_esde_picker_scale_label),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EsdeContentScale.entries.forEach { scale ->
                EsdeContentScaleChip(
                    label = stringResource(
                        when (scale) {
                            EsdeContentScale.FIT -> R.string.canvas_esde_picker_scale_fit
                            EsdeContentScale.CROP -> R.string.canvas_esde_picker_scale_crop
                        }
                    ),
                    isSelected = scale == selected,
                    onClick = { onSelected(scale) }
                )
            }
        }
    }
}

@Composable
private fun EsdeContentScaleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .widthIn(min = 80.dp)
            .height(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .background(
                color = when {
                    isSelected -> ThemePrimaryColor.copy(alpha = 0.7f)
                    isFocused -> ThemePrimaryColor.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected || isFocused) 1.dp else 0.dp,
                color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun HeaderActionButton(
    label: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val baseAlpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isPrimary) ThemePrimaryColor.copy(alpha = 0.85f * baseAlpha)
                else Color.White.copy(alpha = 0.08f * baseAlpha),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.3f * baseAlpha),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = baseAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
