package jr.brian.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.components.onboarding.ShadeSliderOnboardingOverlay
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalOnboardingManager

internal val ShadeBackgroundPresetColors: List<Long> = listOf(
    0xFF111111L,
    0xFF000000L,
    0xFF1E1E2E,
    0xFF0F3460,
    0xFF1F4D2B,
    0xFF3A1F4D,
    0xFF4D1F1F,
    0xFF4D3A1F
)

internal val ShadeAccentPresetColors: List<Long> = listOf(
    0xFFE94560L,
    0xFFFF8A65L,
    0xFFFFD740L,
    0xFF69F0AEL,
    0xFF40C4FFL,
    0xFF7C4DFFL,
    0xFFFF4081L,
    0xFFFFFFFFL
)

@Composable
internal fun ShadeCustomizePage(
    backgroundColorArgb: Long,
    onBackgroundColorChange: (Long) -> Unit,
    cornerRadiusDp: Int,
    onCornerRadiusChange: (Int) -> Unit,
    backgroundAlpha: Float,
    onBackgroundAlphaChange: (Float) -> Unit,
    accentColorArgb: Long,
    onAccentColorChange: (Long) -> Unit,
    accentColor: Color
) {
    var showBackgroundColorDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }

    val onboardingManager = LocalOnboardingManager.current
    val hasSeenSliderHint by onboardingManager.hasSeenSliderHint.collectAsStateWithLifecycle()
    var cornerRadiusCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var opacityCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val cornerRadiusSlider: @Composable (Modifier) -> Unit = { sliderModifier ->
        CornerRadiusSliderRow(
            cornerRadiusDp = cornerRadiusDp,
            accentColor = accentColor,
            onCornerRadiusChange = onCornerRadiusChange,
            modifier = sliderModifier
        )
    }
    val opacitySlider: @Composable (Modifier) -> Unit = { sliderModifier ->
        BackgroundOpacitySliderRow(
            backgroundAlpha = backgroundAlpha,
            accentColor = accentColor,
            onBackgroundAlphaChange = onBackgroundAlphaChange,
            modifier = sliderModifier
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackgroundColorRow(
                    backgroundColorArgb = backgroundColorArgb,
                    accentColor = accentColor,
                    onClick = { showBackgroundColorDialog = true },
                    modifier = Modifier.weight(1f)
                )
                AccentColorRow(
                    accentColorArgb = accentColorArgb,
                    accentColor = accentColor,
                    onClick = { showAccentColorDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
            cornerRadiusSlider(
                Modifier.onGloballyPositioned { cornerRadiusCoordinates = it }
            )
            opacitySlider(
                Modifier.onGloballyPositioned { opacityCoordinates = it }
            )
        }

        if (!hasSeenSliderHint) {
            ShadeSliderOnboardingOverlay(
                cornerRadiusCoordinates = cornerRadiusCoordinates,
                opacityCoordinates = opacityCoordinates,
                cornerRadiusContent = { cornerRadiusSlider(Modifier) },
                opacityContent = { opacitySlider(Modifier) },
                onComplete = { onboardingManager.markSliderHintSeen() }
            )
        }
    }

    if (showBackgroundColorDialog) {
        ShadeColorPickerDialog(
            title = stringResource(R.string.shade_pick_color),
            selectedArgb = backgroundColorArgb,
            presetColors = ShadeBackgroundPresetColors,
            accentColor = accentColor,
            includeThemeDefault = false,
            onColorSelected = {
                onBackgroundColorChange(it)
                showBackgroundColorDialog = false
            },
            onReset = {
                onBackgroundColorChange(GridSettingsManager.DEFAULT_SHADE_BACKGROUND_COLOR_ARGB)
                showBackgroundColorDialog = false
            },
            onDismiss = { showBackgroundColorDialog = false }
        )
    }

    if (showAccentColorDialog) {
        ShadeColorPickerDialog(
            title = stringResource(R.string.shade_pick_accent_color),
            selectedArgb = accentColorArgb,
            presetColors = ShadeAccentPresetColors,
            accentColor = accentColor,
            includeThemeDefault = true,
            onColorSelected = {
                onAccentColorChange(it)
                showAccentColorDialog = false
            },
            onReset = {
                onAccentColorChange(GridSettingsManager.DEFAULT_SHADE_ACCENT_COLOR_ARGB)
                showAccentColorDialog = false
            },
            onDismiss = { showAccentColorDialog = false }
        )
    }
}

@Composable
private fun BackgroundColorRow(
    backgroundColorArgb: Long,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingClickableRow(
        icon = Icons.Default.Palette,
        accentColor = accentColor,
        title = stringResource(R.string.shade_background_color),
        description = null,
        onClick = onClick,
        trailing = { ColorPreviewSwatch(color = Color(backgroundColorArgb)) },
        modifier = modifier
    )
}

@Composable
private fun AccentColorRow(
    accentColorArgb: Long,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingClickableRow(
        icon = Icons.Default.ColorLens,
        accentColor = accentColor,
        title = stringResource(R.string.shade_accent_color),
        description = null,
        onClick = onClick,
        trailing = {
            if (accentColorArgb == GridSettingsManager.DEFAULT_SHADE_ACCENT_COLOR_ARGB) {
                ColorPreviewSwatch(color = accentColor, label = "A")
            } else {
                ColorPreviewSwatch(color = Color(accentColorArgb))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CornerRadiusSliderRow(
    cornerRadiusDp: Int,
    accentColor: Color,
    onCornerRadiusChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSliderRow(
        icon = Icons.Default.RoundedCorner,
        accentColor = accentColor,
        title = stringResource(R.string.shade_corner_radius),
        valueLabel = "$cornerRadiusDp dp",
        value = cornerRadiusDp.toFloat(),
        valueRange = GridSettingsManager.MIN_SHADE_CORNER_RADIUS_DP.toFloat()
                ..GridSettingsManager.MAX_SHADE_CORNER_RADIUS_DP.toFloat(),
        steps = GridSettingsManager.MAX_SHADE_CORNER_RADIUS_DP -
                GridSettingsManager.MIN_SHADE_CORNER_RADIUS_DP - 1,
        onValueChange = { onCornerRadiusChange(it.toInt()) },
        modifier = modifier
    )
}

@Composable
private fun BackgroundOpacitySliderRow(
    backgroundAlpha: Float,
    accentColor: Color,
    onBackgroundAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingSliderRow(
        icon = Icons.Default.Opacity,
        accentColor = accentColor,
        title = stringResource(R.string.shade_background_opacity),
        valueLabel = "${(backgroundAlpha * 100).toInt()}%",
        value = backgroundAlpha,
        valueRange = GridSettingsManager.MIN_SHADE_BACKGROUND_ALPHA
                ..GridSettingsManager.MAX_SHADE_BACKGROUND_ALPHA,
        steps = 13,
        onValueChange = onBackgroundAlphaChange,
        modifier = modifier
    )
}

@Composable
private fun SettingClickableRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (description != null) {
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SettingSliderRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        AutoLockingSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun ColorPreviewSwatch(color: Color, label: String? = null) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ShadeColorPickerDialog(
    title: String,
    selectedArgb: Long,
    presetColors: List<Long>,
    accentColor: Color,
    includeThemeDefault: Boolean,
    onColorSelected: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(color = OledCardColor, shape = RoundedCornerShape(16.dp))
                .border(
                    width = 2.dp,
                    color = accentColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                ShadeColorGrid(
                    selectedArgb = selectedArgb,
                    presetColors = presetColors,
                    accentColor = accentColor,
                    includeThemeDefault = includeThemeDefault,
                    themeDefaultColor = accentColor,
                    onColorSelected = onColorSelected
                )
                ShadeDialogActions(
                    onReset = onReset,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ShadeColorGrid(
    selectedArgb: Long,
    presetColors: List<Long>,
    accentColor: Color,
    includeThemeDefault: Boolean,
    themeDefaultColor: Color,
    onColorSelected: (Long) -> Unit
) {
    val tiles = buildList {
        if (includeThemeDefault) {
            add(ShadeColorTile.ThemeDefault)
        }
        addAll(presetColors.map { ShadeColorTile.Preset(it) })
    }
    val rows = tiles.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowTiles.forEach { tile ->
                    when (tile) {
                        is ShadeColorTile.ThemeDefault -> ShadeColorSwatch(
                            color = themeDefaultColor,
                            isSelected = selectedArgb == GridSettingsManager.DEFAULT_SHADE_ACCENT_COLOR_ARGB,
                            accentColor = accentColor,
                            label = "A",
                            onClick = { onColorSelected(GridSettingsManager.DEFAULT_SHADE_ACCENT_COLOR_ARGB) },
                            modifier = Modifier.weight(1f)
                        )
                        is ShadeColorTile.Preset -> ShadeColorSwatch(
                            color = Color(tile.argb),
                            isSelected = tile.argb == selectedArgb,
                            accentColor = accentColor,
                            onClick = { onColorSelected(tile.argb) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat(4 - rowTiles.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private sealed class ShadeColorTile {
    data object ThemeDefault : ShadeColorTile()
    data class Preset(val argb: Long) : ShadeColorTile()
}

@Composable
private fun ShadeColorSwatch(
    color: Color,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ShadeDialogActions(
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShadeDialogActionButton(
            label = stringResource(R.string.shade_reset_color),
            onClick = onReset,
            modifier = Modifier.weight(1f)
        )
        ShadeDialogActionButton(
            label = stringResource(R.string.common_close),
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShadeDialogActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
