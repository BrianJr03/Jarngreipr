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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Add-action choices surfaced by [CanvasMainDialog]'s options list. Mirrors
 * the variants of [jr.brian.home.canvas.model.CanvasItem] one-to-one.
 */
enum class CanvasAddChoice {
    APP, FOLDER, ROM, WIDGET, RSS_LAUNCHER, ESDE_DISPLAY
}

private enum class CanvasDialogMode { Options, Edit }

/**
 * The single canvas dialog. Mode 1 is a compact list of actions (Edit Canvas
 * first, then add actions); tapping a row either invokes its action and
 * dismisses, or — for Edit Canvas — switches the dialog body to host
 * [EditCanvasContent] in-place with a back affordance.
 *
 * Replaces the old pair of `CanvasAddItemDialog` (square-tile grid) and
 * `CanvasEditDialog` (standalone). There is now one surface, one entry point
 * (the canvas's add icon), and one focus traversal across all controls.
 */
@Composable
fun CanvasMainDialog(
    layout: CanvasLayout,
    onChoice: (CanvasAddChoice) -> Unit,
    onOrientationChanged: (CanvasScrollOrientation) -> Unit,
    onGridChanged: (columns: Int, rows: Int) -> Unit,
    onEditModeChanged: (Boolean) -> Unit,
    onTidy: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(CanvasDialogMode.Options) }
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
                .heightIn(max = 560.dp)
                .border(
                    width = 1.dp,
                    brush = borderBrush(isFocused = true),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (mode) {
                    CanvasDialogMode.Options -> OptionsBody(
                        onEditCanvasClick = { mode = CanvasDialogMode.Edit },
                        onChoice = { choice ->
                            onChoice(choice)
                            onDismiss()
                        }
                    )
                    CanvasDialogMode.Edit -> EditBody(
                        layout = layout,
                        onBack = { mode = CanvasDialogMode.Options },
                        onOrientationChanged = onOrientationChanged,
                        onGridChanged = onGridChanged,
                        onEditModeChanged = onEditModeChanged,
                        onTidy = {
                            onTidy()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsBody(
    onEditCanvasClick: () -> Unit,
    onChoice: (CanvasAddChoice) -> Unit
) {
    Text(
        text = stringResource(R.string.canvas_dialog_title),
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
    CompactOptionRow(
        icon = Icons.Default.Tune,
        labelRes = R.string.canvas_edit_canvas_option,
        isPrimary = true,
        onClick = onEditCanvasClick
    )
    HairlineSeparator()
    CANVAS_ADD_TILES.forEach { tile ->
        CompactOptionRow(
            icon = tile.icon,
            labelRes = tile.labelRes,
            isPrimary = false,
            onClick = { onChoice(tile.choice) }
        )
    }
}

@Composable
private fun EditBody(
    layout: CanvasLayout,
    onBack: () -> Unit,
    onOrientationChanged: (CanvasScrollOrientation) -> Unit,
    onGridChanged: (columns: Int, rows: Int) -> Unit,
    onEditModeChanged: (Boolean) -> Unit,
    onTidy: () -> Unit
) {
    EditBodyHeader(onBack = onBack)
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditCanvasContent(
            layout = layout,
            onOrientationChanged = onOrientationChanged,
            onGridChanged = onGridChanged,
            onEditModeChanged = onEditModeChanged,
            onTidy = onTidy
        )
    }
}

@Composable
private fun EditBodyHeader(onBack: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged { isFocused = it.isFocused }
                .background(
                    brush = cardGradient(isFocused = isFocused),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    brush = borderBrush(isFocused = isFocused),
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() }
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.canvas_dialog_back),
                tint = ThemePrimaryColor
            )
        }
        Text(
            text = stringResource(R.string.canvas_edit_dialog_title),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HairlineSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color.White.copy(alpha = 0.08f))
            .heightIn(min = 1.dp, max = 1.dp)
    )
}

@Composable
private fun CompactOptionRow(
    icon: ImageVector,
    labelRes: Int,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPrimary) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = stringResource(labelRes),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class AddOptionTileSpec(
    val choice: CanvasAddChoice,
    val icon: ImageVector,
    val labelRes: Int
)

private val CANVAS_ADD_TILES: List<AddOptionTileSpec> = listOf(
    AddOptionTileSpec(CanvasAddChoice.APP, Icons.Default.Apps, R.string.canvas_add_app_option),
    AddOptionTileSpec(CanvasAddChoice.FOLDER, Icons.Default.Folder, R.string.canvas_add_folder_option),
    AddOptionTileSpec(CanvasAddChoice.ROM, Icons.Default.VideogameAsset, R.string.canvas_add_rom_option),
    AddOptionTileSpec(CanvasAddChoice.WIDGET, Icons.Default.Widgets, R.string.canvas_add_widget_option),
    AddOptionTileSpec(CanvasAddChoice.RSS_LAUNCHER, Icons.Default.RssFeed, R.string.canvas_add_rss_launcher_option),
    AddOptionTileSpec(CanvasAddChoice.ESDE_DISPLAY, Icons.Default.Image, R.string.canvas_add_esde_display_option)
)
