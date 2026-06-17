package jr.brian.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

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

@Composable
internal fun ShadeCustomizePage(
    backgroundColorArgb: Long,
    onBackgroundColorChange: (Long) -> Unit
) {
    var showColorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackgroundColorRow(
            backgroundColorArgb = backgroundColorArgb,
            onClick = { showColorDialog = true }
        )
    }

    if (showColorDialog) {
        ShadeColorPickerDialog(
            selectedArgb = backgroundColorArgb,
            onColorSelected = {
                onBackgroundColorChange(it)
                showColorDialog = false
            },
            onReset = {
                onBackgroundColorChange(GridSettingsManager.DEFAULT_SHADE_BACKGROUND_COLOR_ARGB)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
}

@Composable
private fun BackgroundColorRow(
    backgroundColorArgb: Long,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.shade_background_color),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.shade_background_color_description),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        ColorPreviewSwatch(colorArgb = backgroundColorArgb)
    }
}

@Composable
private fun ColorPreviewSwatch(colorArgb: Long) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
    )
}

@Composable
private fun ShadeColorPickerDialog(
    selectedArgb: Long,
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
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.shade_pick_color),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                ShadeColorGrid(
                    selectedArgb = selectedArgb,
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
    onColorSelected: (Long) -> Unit
) {
    val rows = ShadeBackgroundPresetColors.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowColors.forEach { argb ->
                    ShadeColorSwatch(
                        colorArgb = argb,
                        isSelected = argb == selectedArgb,
                        onClick = { onColorSelected(argb) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowColors.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ShadeColorSwatch(
    colorArgb: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    )
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
