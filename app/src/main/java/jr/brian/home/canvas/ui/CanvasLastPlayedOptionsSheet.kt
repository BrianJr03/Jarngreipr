package jr.brian.home.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.dialog.AppBottomSheet
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Edit sheet for an existing [CanvasItem.LastPlayedItem]. Mirrors the shape of
 * [CanvasFrontendArtChooserSheet] — a scroll column of selectors over the
 * tile's configuration, with Remove / Cancel / Apply at the bottom. Reuses the
 * same section composables (Fit/Crop, background colour, corner radius, header
 * action buttons) so the two sheets feel like one surface.
 */
@Composable
fun CanvasLastPlayedOptionsSheet(
    initialCount: Int,
    initialShowRandomButton: Boolean,
    initialContentScale: EsdeContentScale,
    initialBackgroundColorArgb: Long?,
    initialBackgroundCornerRadiusDp: Int?,
    onConfirm: (count: Int, showRandomButton: Boolean, scale: EsdeContentScale, bgArgb: Long?, cornerDp: Int?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember(initialCount) { mutableIntStateOf(initialCount) }
    var showRandom by remember(initialShowRandomButton) { mutableStateOf(initialShowRandomButton) }
    var scale by remember(initialContentScale) { mutableStateOf(initialContentScale) }
    var bgArgb by remember(initialBackgroundColorArgb) { mutableStateOf(initialBackgroundColorArgb) }
    var cornerDp by remember(initialBackgroundCornerRadiusDp) {
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
                title = stringResource(R.string.canvas_last_played_edit_title),
                onDismiss = onDismiss
            )

            RowCountStepper(
                value = count,
                onChange = { count = it }
            )

            RandomToggleRow(
                checked = showRandom,
                onCheckedChange = { showRandom = it }
            )

            EsdeContentScaleSelector(
                selected = scale,
                onSelected = { scale = it }
            )

            BackgroundColorSelector(
                selectedArgb = bgArgb,
                onColorSelected = { bgArgb = it }
            )

            CornerRadiusSelector(
                enabled = bgArgb != null,
                valueDp = cornerDp,
                onValueChange = { cornerDp = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                HeaderActionButton(
                    label = stringResource(R.string.canvas_remove_item_title),
                    isPrimary = false,
                    enabled = true,
                    onClick = {
                        onRemove()
                        onDismiss()
                    }
                )
                HeaderActionButton(
                    label = stringResource(R.string.canvas_picker_cancel),
                    isPrimary = false,
                    enabled = true,
                    onClick = onDismiss
                )
                HeaderActionButton(
                    label = stringResource(R.string.canvas_last_played_apply),
                    isPrimary = true,
                    enabled = true,
                    onClick = {
                        onConfirm(count, showRandom, scale, bgArgb, bgArgb?.let { cornerDp })
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun RowCountStepper(
    value: Int,
    onChange: (Int) -> Unit
) {
    val min = CanvasItem.LastPlayedItem.MIN_DISPLAY_COUNT
    val max = CanvasItem.LastPlayedItem.MAX_DISPLAY_COUNT
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.canvas_last_played_count_label),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                enabled = value > min,
                icon = Icons.Default.Remove,
                contentDescription = stringResource(R.string.canvas_last_played_count_decrement),
                onClick = { onChange((value - 1).coerceAtLeast(min)) }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.width(16.dp))
            StepperButton(
                enabled = value < max,
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.canvas_last_played_count_increment),
                onClick = { onChange((value + 1).coerceAtMost(max)) }
            )
        }
    }
}

@Composable
private fun StepperButton(
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.3f
    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = ThemePrimaryColor.copy(alpha = 0.3f * alpha),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.6f * alpha),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = alpha)
        )
    }
}

@Composable
private fun RandomToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.canvas_last_played_random_label),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ThemePrimaryColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * Compact bottom sheet surfaced when the user long-presses a Last Played
 * entry row. Offers Launch and Remove From List as the two bulk operations
 * applicable to a single launch-history entry; Pin to Canvas and Game Details
 * are not wired through this surface (plan calls them nice-to-haves).
 */
@Composable
fun LastPlayedRowOptionsSheet(
    gameName: String,
    onLaunch: () -> Unit,
    onRemoveFromList: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = gameName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            RowAction(
                label = stringResource(R.string.canvas_last_played_row_launch),
                isPrimary = true,
                onClick = {
                    onLaunch()
                    onDismiss()
                }
            )
            RowAction(
                label = stringResource(R.string.canvas_last_played_remove_from_list),
                isPrimary = false,
                onClick = {
                    onRemoveFromList()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun RowAction(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isPrimary) ThemePrimaryColor.copy(alpha = 0.75f)
                else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.9f)
                else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
