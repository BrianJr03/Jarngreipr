package jr.brian.home.canvas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.WallpaperOptionsSection
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.util.MediaPickerLauncher

/**
 * Inner controls for editing a canvas page (edit-mode toggle, orientation,
 * column/row steppers, tidy). Lives standalone so the unified canvas dialog
 * can host it as a sub-view; the old standalone `CanvasEditDialog` surface
 * is gone (one dialog now, see [CanvasMenuDialog]).
 */
@Composable
fun ColumnScope.EditCanvasContent(
    layout: CanvasLayout,
    onOrientationChanged: (CanvasScrollOrientation) -> Unit,
    onGridChanged: (columns: Int, rows: Int) -> Unit,
    onEditModeChanged: (Boolean) -> Unit,
    onTidy: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    var orientation by remember(layout.activeOrientation) { mutableStateOf(layout.activeOrientation) }
    var columns by remember(layout.verticalColumns) { mutableStateOf(layout.verticalColumns) }
    var rows by remember(layout.horizontalRows) { mutableStateOf(layout.horizontalRows) }
    var editMode by remember(layout.editMode) { mutableStateOf(layout.editMode) }

    var isWallpaperExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val setupPreferences = remember { SetupPreferences(context) }
    val mediaPickerLauncher = MediaPickerLauncher(
        onResult = {
            isWallpaperExpanded = false
            onDismiss()
        }
    )

    AnimatedVisibility(
        visible = !isWallpaperExpanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            WallpaperControl(onChangeClick = { isWallpaperExpanded = true })

            EditModeToggle(
                enabled = editMode,
                onToggle = {
                    editMode = it
                    onEditModeChanged(it)
                }
            )

            OrientationToggle(
                selected = orientation,
                onSelected = {
                    orientation = it
                    onOrientationChanged(it)
                }
            )

            AxisStepper(
                label = stringResource(R.string.canvas_columns_label),
                value = columns,
                onValueChange = {
                    columns = it
                    onGridChanged(columns, rows)
                }
            )

            AxisStepper(
                label = stringResource(R.string.canvas_rows_label),
                value = rows,
                onValueChange = {
                    rows = it
                    onGridChanged(columns, rows)
                }
            )

            TidyControl(onTidy = onTidy)
        }
    }

    WallpaperOptionsSection(
        isVisible = isWallpaperExpanded,
        wallpaperManager = wallpaperManager,
        setupPreferences = setupPreferences,
        mediaPickerLauncher = mediaPickerLauncher,
        onBack = { isWallpaperExpanded = false },
        onDismiss = onDismiss,
        onESDESetupClick = {}
    )
}

@Composable
private fun WallpaperControl(onChangeClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_wallpaper_label),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        OrientationChip(
            label = stringResource(R.string.canvas_change_wallpaper),
            isSelected = false,
            onClick = onChangeClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TidyControl(onTidy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_tidy_label),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        OrientationChip(
            label = stringResource(R.string.canvas_tidy_button),
            isSelected = false,
            onClick = onTidy,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.canvas_tidy_hint),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EditModeToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_edit_mode_label),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrientationChip(
                label = stringResource(R.string.canvas_edit_mode_on),
                isSelected = enabled,
                onClick = { onToggle(true) },
                modifier = Modifier.weight(1f)
            )
            OrientationChip(
                label = stringResource(R.string.canvas_edit_mode_off),
                isSelected = !enabled,
                onClick = { onToggle(false) },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(
                if (enabled) R.string.canvas_edit_mode_on_hint
                else R.string.canvas_edit_mode_off_hint
            ),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun OrientationToggle(
    selected: CanvasScrollOrientation,
    onSelected: (CanvasScrollOrientation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_orientation_label),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vertical scroll is hidden for now — horizontal is the only
            // supported orientation while we tune the vertical mode UX.
            // OrientationChip(
            //     label = stringResource(R.string.canvas_orientation_vertical),
            //     isSelected = selected == CanvasScrollOrientation.VERTICAL,
            //     onClick = { onSelected(CanvasScrollOrientation.VERTICAL) },
            //     modifier = Modifier.weight(1f)
            // )
            OrientationChip(
                label = stringResource(R.string.canvas_orientation_horizontal),
                isSelected = selected == CanvasScrollOrientation.HORIZONTAL,
                onClick = { onSelected(CanvasScrollOrientation.HORIZONTAL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OrientationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused || isSelected),
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isSelected || isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isSelected || isFocused),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .focusable()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ThemePrimaryColor else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AxisStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepperButton(
                icon = Icons.Default.KeyboardArrowDown,
                enabled = value > CanvasLayout.MIN_AXIS,
                onClick = {
                    if (value > CanvasLayout.MIN_AXIS) onValueChange(value - 1)
                }
            )
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            StepperButton(
                icon = Icons.Default.KeyboardArrowUp,
                enabled = value < CanvasLayout.MAX_AXIS,
                onClick = {
                    if (value < CanvasLayout.MAX_AXIS) onValueChange(value + 1)
                }
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemePrimaryColor.copy(alpha = alpha)
        )
    }
}
