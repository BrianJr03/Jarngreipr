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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import jr.brian.home.R
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.AutoLockingSlider
import jr.brian.home.ui.components.dialog.AppBottomSheet
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Preset background colors offered for the ES-DE Display tile's optional box
 * background. Mirrors the palette used by [FolderBackgroundDialog] so the two
 * pickers feel like the same design language.
 */
private val FrontendBackgroundPresetColors: List<Long> = listOf(
    0xFF000000L,
    0xFF111111L,
    0xFF1E1E2E,
    0xFF0F3460,
    0xFF1F4D2B,
    0xFF3A1F4D,
    0xFF4D1F1F,
    0xFF4D3A1F,
    0xFFE94560L,
    0xFFC97B12L,
    0xFF2E7D32L,
    0xFF6A1B9A
)

/**
 * Single chooser for the ES-DE Display tile's [GameImageType], scale, and
 * optional background box (color + corner radius). Used both when adding a new
 * tile and when re-typing an existing one. Confirm always fires [onConfirm]
 * then [onDismiss], so the sheet always closes on a successful selection.
 */
@Composable
fun CanvasFrontendArtChooserSheet(
    initialType: GameImageType,
    initialContentScale: EsdeContentScale,
    initialBackgroundColorArgb: Long?,
    initialBackgroundCornerRadiusDp: Int?,
    titleRes: Int,
    onConfirm: (GameImageType, EsdeContentScale, Long?, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(initialType) { mutableStateOf(initialType) }
    var selectedScale by remember(initialContentScale) { mutableStateOf(initialContentScale) }
    var selectedBackgroundArgb by remember(initialBackgroundColorArgb) {
        mutableStateOf(initialBackgroundColorArgb)
    }
    var selectedCornerRadiusDp by remember(initialBackgroundCornerRadiusDp) {
        mutableIntStateOf(
            initialBackgroundCornerRadiusDp
                ?: CanvasItem.EsdeArtItem.DEFAULT_BACKGROUND_CORNER_RADIUS_DP
        )
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChooserSheetHeader(
                title = stringResource(titleRes),
                onDismiss = onDismiss
            )

            GameImageTypeSelector(
                selectedType = selected,
                onTypeSelected = { selected = it }
            )

            EsdeContentScaleSelector(
                selected = selectedScale,
                onSelected = { selectedScale = it }
            )

            BackgroundColorSelector(
                selectedArgb = selectedBackgroundArgb,
                onColorSelected = { selectedBackgroundArgb = it }
            )

            CornerRadiusSelector(
                enabled = selectedBackgroundArgb != null,
                valueDp = selectedCornerRadiusDp,
                onValueChange = { selectedCornerRadiusDp = it }
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
                        onConfirm(
                            selected,
                            selectedScale,
                            selectedBackgroundArgb,
                            selectedBackgroundArgb?.let { selectedCornerRadiusDp }
                        )
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ChooserSheetHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        HeaderCloseButton(onDismiss = onDismiss)
    }
}

@Composable
private fun HeaderCloseButton(onDismiss: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(CircleShape)
            .clickable { onDismiss() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.dialog_cancel),
            tint = Color.White
        )
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
private fun BackgroundColorSelector(
    selectedArgb: Long?,
    onColorSelected: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_esde_picker_background_label),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        BackgroundSwatchGrid(
            selectedArgb = selectedArgb,
            onColorSelected = onColorSelected
        )
    }
}

@Composable
private fun BackgroundSwatchGrid(
    selectedArgb: Long?,
    onColorSelected: (Long?) -> Unit
) {
    val rows = FrontendBackgroundPresetColors.chunked(6)
    // 8dp matches the rest of the sheet — top-level sections use spacedBy(16),
    // section header→content uses spacedBy(8), chip rows use spacedBy(8). The
    // grid was on its own 12dp rhythm which read as inconsistent gaps.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoBackgroundSwatch(
                isSelected = selectedArgb == null,
                onClick = { onColorSelected(null) }
            )
        }
        rows.forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowColors.forEach { argb ->
                    BackgroundColorSwatch(
                        color = Color(argb),
                        isSelected = selectedArgb == argb,
                        onClick = { onColorSelected(argb) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val highlight = isSelected || isFocused
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(color = color, shape = CircleShape)
            .border(
                width = if (highlight) 3.dp else 1.dp,
                color = if (highlight) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .focusable()
    )
}

@Composable
private fun NoBackgroundSwatch(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val highlight = isSelected || isFocused
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(color = Color.Transparent, shape = CircleShape)
            .border(
                width = if (highlight) 3.dp else 1.dp,
                color = if (highlight) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.canvas_esde_picker_background_none),
            tint = Color.White.copy(alpha = if (highlight) 1f else 0.5f)
        )
    }
}

@Composable
private fun CornerRadiusSelector(
    enabled: Boolean,
    valueDp: Int,
    onValueChange: (Int) -> Unit
) {
    val labelAlpha = if (enabled) 1f else 0.4f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.canvas_esde_picker_corner_radius_label),
                color = Color.White.copy(alpha = labelAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.canvas_esde_picker_corner_radius_value,
                    valueDp
                ),
                color = ThemePrimaryColor.copy(alpha = labelAlpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        val min = CanvasItem.EsdeArtItem.MIN_BACKGROUND_CORNER_RADIUS_DP
        val max = CanvasItem.EsdeArtItem.MAX_BACKGROUND_CORNER_RADIUS_DP
        AutoLockingSlider(
            value = valueDp.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
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
