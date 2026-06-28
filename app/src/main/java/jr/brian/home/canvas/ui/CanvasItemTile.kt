package jr.brian.home.canvas.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.resolveDisplayPath
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import java.io.File

/**
 * Render a [ResolvedCanvasItem] inside a single grid cell. Long-press triggers
 * the canvas's edit menu via [onLongPress]; tap launches the item via [onTap].
 */
@Composable
fun CanvasItemTile(
    resolved: ResolvedCanvasItem,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    suppressTileLongPress: Boolean = false,
    editMode: Boolean = false,
    isDropTarget: Boolean = false,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit = {}
) {
    val effectiveLongPress: () -> Unit = if (suppressTileLongPress) ({}) else onLongPress
    val baseModifier = modifier.then(if (isDropTarget) dropTargetHighlight() else Modifier)
    when (resolved) {
        is ResolvedCanvasItem.App -> CanvasAppTile(
            app = resolved.info,
            fallbackPackage = resolved.raw.packageName,
            onTap = onTap,
            onLongPress = effectiveLongPress,
            modifier = baseModifier,
            editMode = editMode
        )
        is ResolvedCanvasItem.Folder -> CanvasFolderTile(
            folder = resolved.folder,
            fallbackId = resolved.raw.folderId,
            onTap = onTap,
            onLongPress = effectiveLongPress,
            modifier = baseModifier,
            editMode = editMode
        )
        is ResolvedCanvasItem.Rom -> CanvasRomTile(
            rom = resolved.info,
            fallbackKey = resolved.raw.romKey,
            onTap = onTap,
            onLongPress = effectiveLongPress,
            modifier = baseModifier,
            editMode = editMode
        )
        is ResolvedCanvasItem.Widget -> CanvasWidgetTile(
            widgetId = resolved.raw.widgetId,
            onLongPress = onLongPress,
            modifier = baseModifier,
            appWidgetHost = appWidgetHost,
            editMode = editMode,
            onEditTap = { onResizeWidget(resolved.raw) }
        )
        is ResolvedCanvasItem.RssLauncher -> CanvasRssLauncherTile(
            onTap = onTap,
            onLongPress = effectiveLongPress,
            modifier = baseModifier,
            editMode = editMode
        )
    }
}

/** Visual highlight for the slot the user is hovering over during a drag. */
@Composable
private fun dropTargetHighlight(): Modifier =
    Modifier
        .background(
            color = ThemePrimaryColor.copy(alpha = 0.18f),
            shape = RoundedCornerShape(14.dp)
        )
        .border(
            width = 3.dp,
            color = ThemePrimaryColor,
            shape = RoundedCornerShape(14.dp)
        )

@Composable
private fun CanvasAppTile(
    app: AppInfo?,
    fallbackPackage: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    val visibility = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current
    BaseTile(
        onTap = onTap,
        onLongPress = onLongPress,
        modifier = modifier,
        editMode = editMode
    ) {
        if (app != null) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier = Modifier.size(56.dp)
            )
            if (visibility.showHomeScreenAppNames) {
                Spacer(Modifier.height(4.dp))
                TileLabel(app.label)
            }
        } else {
            MissingTile(label = fallbackPackage, icon = Icons.Default.Apps)
        }
    }
}

@Composable
private fun CanvasFolderTile(
    folder: Folder?,
    fallbackId: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    val visibility = LocalAppVisibilityManager.current
    BaseTile(
        onTap = onTap,
        onLongPress = onLongPress,
        modifier = modifier,
        editMode = editMode
    ) {
        if (folder != null) {
            FolderPreviewBox(folder = folder)
            if (visibility.showFolderNames) {
                Spacer(Modifier.height(4.dp))
                TileLabel(folder.name)
            }
        } else {
            MissingTile(label = fallbackId, icon = Icons.Default.Apps)
        }
    }
}

@Composable
private fun FolderPreviewBox(folder: Folder) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OledCardColor.copy(alpha = 0.9f))
            .border(2.dp, ThemePrimaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = folder.appPackageNames.size.toString(),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CanvasRomTile(
    rom: PinnedRomInfo?,
    fallbackKey: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    val context = LocalContext.current
    val visibility = LocalAppVisibilityManager.current
    BaseTile(
        onTap = onTap,
        onLongPress = onLongPress,
        modifier = modifier,
        editMode = editMode
    ) {
        if (rom != null) {
            val artwork = rom.resolveDisplayPath()
                ?.let { File(it) }
                ?.takeIf { it.exists() }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (artwork != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artwork)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.rom_search_icon_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = stringResource(R.string.rom_search_icon_description),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            if (visibility.showHomeScreenAppNames) {
                Spacer(Modifier.height(4.dp))
                TileLabel(rom.name)
            }
        } else {
            MissingTile(label = fallbackKey, icon = Icons.Default.VideogameAsset)
        }
    }
}

@Composable
private fun CanvasWidgetTile(
    widgetId: Int,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    editMode: Boolean = false,
    onEditTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val providerInfo = remember(widgetId) {
        AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)
    }
    BaseTile(
        onTap = {},
        onLongPress = onLongPress,
        modifier = modifier,
        editMode = editMode
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(OledCardColor.copy(alpha = 0.85f))
                .border(
                    width = if (editMode) 3.dp else 2.dp,
                    color = if (editMode) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (appWidgetHost != null && providerInfo != null) {
                AndroidView(
                    factory = { ctx ->
                        appWidgetHost.createView(ctx, widgetId, providerInfo)
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { it.requestLayout() }
                )
                if (editMode) {
                    // Absorb taps so the widget's own click handlers don't fire while
                    // editing. Resize is via the corner handle (touch + D-pad);
                    // [onEditTap] is retained for source compat but unused.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ThemePrimaryColor.copy(alpha = 0.15f))
                            .clickable { }
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = stringResource(R.string.canvas_widget_placeholder_description),
                        tint = ThemePrimaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.canvas_widget_id_label, widgetId),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasRssLauncherTile(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    BaseTile(
        onTap = onTap,
        onLongPress = onLongPress,
        modifier = modifier,
        editMode = editMode
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledCardColor.copy(alpha = 0.9f))
                .border(2.dp, ThemePrimaryColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = stringResource(R.string.canvas_rss_launcher_tile),
                tint = ThemePrimaryColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        TileLabel(stringResource(R.string.canvas_rss_launcher_tile))
    }
}

@Composable
private fun MissingTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Red.copy(alpha = 0.15f))
            .border(2.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.canvas_missing_item_description),
            tint = Color.Red.copy(alpha = 0.8f),
            modifier = Modifier.size(28.dp)
        )
    }
    Spacer(Modifier.height(4.dp))
    TileLabel(label, color = Color.White.copy(alpha = 0.6f))
}

@Composable
private fun TileLabel(text: String, color: Color = Color.White) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BaseTile(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    // In edit mode, the outer pointerInput on the grid tile owns long-press for
    // drag. Using combinedClickable here would race with that and the gesture
    // would never reach the drag detector (the bug that left apps/folders stuck).
    val clickModifier = if (editMode) {
        Modifier.clickable(onClick = onTap)
    } else {
        Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .then(clickModifier)
            .focusable()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

/** Used by parent to render an "Add" tile inside the same grid cell shape. */
@Composable
fun CanvasAddItemTile(
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseTile(
        onTap = onTap,
        onLongPress = onTap,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OledCardColor.copy(alpha = 0.6f))
                .border(2.dp, ThemePrimaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = ThemePrimaryColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        TileLabel(
            stringResource(R.string.canvas_add_item_tile),
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

