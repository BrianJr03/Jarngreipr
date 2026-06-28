package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * App picker grid. Single-select by default — every callsite that doesn't pass
 * [multiSelect] gets the original immediate-dismiss-on-tap behavior. When
 * [multiSelect] is true, taps toggle a per-item selected state, a header row
 * shows a focusable "Done (N)" action, and confirmation routes through
 * [onMultiSelectConfirm].
 */
@Composable
fun AppSelectionDialog(
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    multiSelect: Boolean = false,
    onMultiSelectConfirm: (List<AppInfo>) -> Unit = {}
) {
    val selected = remember { mutableStateMapOf<String, AppInfo>() }
    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBackgroundColor.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (multiSelect) {
                    MultiSelectHeader(
                        selectedCount = selected.size,
                        onConfirm = {
                            if (selected.isNotEmpty()) {
                                onMultiSelectConfirm(selected.values.toList())
                            }
                        },
                        onCancel = onDismiss
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppSelectionItem(
                            app = app,
                            isSelected = multiSelect && selected.containsKey(app.packageName),
                            onClick = {
                                if (multiSelect) {
                                    if (selected.containsKey(app.packageName)) {
                                        selected.remove(app.packageName)
                                    } else {
                                        selected[app.packageName] = app
                                    }
                                } else {
                                    onAppSelected(app)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiSelectHeader(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.canvas_picker_selected_count, selectedCount),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        HeaderButton(
            label = stringResource(R.string.canvas_picker_cancel),
            isPrimary = false,
            enabled = true,
            onClick = onCancel
        )
        HeaderButton(
            label = stringResource(R.string.canvas_picker_done),
            isPrimary = true,
            enabled = selectedCount > 0,
            onClick = onConfirm
        )
    }
}

@Composable
private fun HeaderButton(
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

@Composable
private fun AppSelectionItem(
    app: AppInfo,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = when {
                    isSelected -> 2.dp
                    isFocused -> 2.dp
                    else -> 0.dp
                },
                color = when {
                    isSelected -> ThemePrimaryColor
                    isFocused -> Color.White.copy(alpha = 0.8f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = app.icon),
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (isSelected) SelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun BoxScope.SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(ThemePrimaryColor, shape = CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}
