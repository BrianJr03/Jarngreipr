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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Steppers for a widget's [CanvasItem.WidgetItem.colSpan]/[rowSpan]. Clamped to
 * [CanvasLayout.MIN_AXIS]/[MAX_AXIS]. Calls [onResize] on every change so the
 * grid reflects the new size live; [onDismiss] when the user closes.
 */
@Composable
fun CanvasResizeWidgetDialog(
    widget: CanvasItem.WidgetItem,
    onResize: (colSpan: Int, rowSpan: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var cols by remember(widget.id) { mutableStateOf(widget.colSpan) }
    var rows by remember(widget.id) { mutableStateOf(widget.rowSpan) }

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
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.canvas_resize_widget_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.canvas_resize_widget_subtitle, cols, rows),
                    color = ThemePrimaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                SpanStepper(
                    label = stringResource(R.string.canvas_resize_widget_cols),
                    value = cols,
                    onValueChange = {
                        cols = it
                        onResize(cols, rows)
                    }
                )
                SpanStepper(
                    label = stringResource(R.string.canvas_resize_widget_rows),
                    value = rows,
                    onValueChange = {
                        rows = it
                        onResize(cols, rows)
                    }
                )
            }
        }
    }
}

@Composable
private fun SpanStepper(
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
                textAlign = TextAlign.Center
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
