package jr.brian.jarnfe.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppIcon
import jr.brian.jarnfe.apps.jarnAppIconSize
import jr.brian.jarnfe.apps.jarnHiddenAppName
import jr.brian.jarnfe.look.rememberJarnDisplay
import java.io.File

private const val FOLDER_PREVIEW_COUNT = 4
private const val FOLDER_PREVIEW_COLUMNS = 2
private val AppIconSize = 44.dp
private val FolderIconSize = 22.dp

@Composable
internal fun BoxScope.CanvasTileContent(
    item: CanvasItem,
    page: JarnCanvasPage,
    snapshot: JarnCanvasSnapshot,
    stageArt: CanvasStageArt?,
) {
    when (item) {
        is CanvasItem.AppItem -> AppTile(page.apps[item.packageName])
        is CanvasItem.FolderItem -> FolderTile(page.folders[item.folderId])
        is CanvasItem.RomItem -> RomTile(item, page.roms[item.romKey])
        is CanvasItem.WidgetItem -> LabelTile(stringResource(R.string.jarnfe_canvas_tile_widget))
        is CanvasItem.RssLauncherItem -> LabelTile(stringResource(R.string.jarnfe_canvas_tile_rss))
        is CanvasItem.RssMusicItem -> MusicTile(snapshot.music)
        is CanvasItem.EsdeArtItem -> ArtTile(item, stageArt)
        is CanvasItem.PhotoContainer -> PhotoTile(item)
        is CanvasItem.LastPlayedItem -> LastPlayedTile(item, snapshot.recent)
    }
}

/** Game art sits straight on the wallpaper. A missing game keeps its card so its label stays readable. */
internal fun CanvasItem.showsCard(page: JarnCanvasPage): Boolean = when (this) {
    is CanvasItem.EsdeArtItem -> !isCardTransparent
    is CanvasItem.RomItem -> page.roms[romKey] == null
    is CanvasItem.LastPlayedItem -> false
    else -> true
}

internal fun EsdeContentScale.toContentScale(): ContentScale = when (this) {
    EsdeContentScale.FIT -> ContentScale.Fit
    EsdeContentScale.CROP -> ContentScale.Crop
}

@Composable
internal fun BoxScope.CanvasImage(
    model: Any?,
    title: String,
    scale: ContentScale,
    imageModifier: Modifier = Modifier,
    placeholder: @Composable BoxScope.(String) -> Unit = {
        JarnArtPlaceholder(it, Modifier.matchParentSize())
    },
) {
    var failed by remember(model) { mutableStateOf(false) }
    if (model == null || failed) {
        placeholder(title)
    } else {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.matchParentSize().then(imageModifier),
            contentScale = scale,
            onError = { failed = true },
        )
    }
}

@Composable
internal fun BoxScope.LabelTile(label: String) {
    JarnText(
        text = label,
        style = JarnTheme.baseType.labelStrong,
        color = JarnTheme.colors.textMid,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(JarnTheme.space.sm),
        maxLines = 3,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BoxScope.MissingTile() = LabelTile(stringResource(R.string.jarnfe_canvas_tile_missing))

@Composable
private fun BoxScope.AppTile(app: JarnCanvasApp?) {
    if (app == null) {
        MissingTile()
        return
    }
    val nameVisible = rememberJarnDisplay().appNamesVisible
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .jarnHiddenAppName(app.label, nameVisible)
            .padding(JarnTheme.space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        CanvasAppIcon(app, Modifier.size(jarnAppIconSize(AppIconSize, nameVisible)))
        if (nameVisible) CanvasAppName(app.label)
    }
}

@Composable
internal fun CanvasAppName(label: String) {
    JarnText(
        text = label,
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textHigh,
        maxLines = 1,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun CanvasAppIcon(app: JarnCanvasApp, modifier: Modifier = Modifier) {
    JarnAppIcon(app.icon, app.iconPath, app.iconVersion, modifier)
}

@Composable
private fun BoxScope.FolderBackground(folder: JarnCanvasFolder) {
    val color = folder.backgroundColorArgb?.let { Color(it) } ?: Color.Transparent
    Box(
        Modifier
            .matchParentSize()
            .background(color),
    ) {
        val image = remember(folder.backgroundImagePath) {
            folder.backgroundImagePath?.let(::File)?.takeIf { it.exists() }
        }
        image?.let { file ->
            AsyncImage(
                model = file,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(JarnTheme.colors.artScrim),
            )
        }
    }
}

@Composable
private fun BoxScope.FolderTile(folder: JarnCanvasFolder?) {
    if (folder == null) {
        MissingTile()
        return
    }
    FolderBackground(folder)
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(JarnTheme.space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        folder.apps.take(FOLDER_PREVIEW_COUNT).chunked(FOLDER_PREVIEW_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
                row.forEach { app ->
                    CanvasAppIcon(app, Modifier.size(FolderIconSize))
                }
            }
        }
        JarnText(
            text = folder.name,
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.RomTile(item: CanvasItem.RomItem, rom: JarnCanvasRom?) {
    if (rom == null) {
        MissingTile()
        return
    }
    CanvasImage(rom.artPath, rom.title, item.resolvedContentScale.toContentScale())
}

@Composable
private fun BoxScope.MusicTile(music: JarnCanvasMusic?) {
    if (music == null) {
        LabelTile(stringResource(R.string.jarnfe_canvas_tile_music))
        return
    }
    music.imageUrl?.let { CanvasImage(it, music.title, ContentScale.Crop) }
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(JarnTheme.colors.artScrim)
            .padding(JarnTheme.space.sm),
    ) {
        JarnText(music.title, JarnTheme.baseType.labelStrong, JarnTheme.colors.textHigh, maxLines = 1)
        music.artist?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1) }
    }
}

@Composable
private fun BoxScope.PhotoTile(item: CanvasItem.PhotoContainer) {
    val uri = item.imageUri
    if (uri == null) {
        LabelTile(stringResource(R.string.jarnfe_canvas_tile_photo))
    } else {
        CanvasImage(uri, stringResource(R.string.jarnfe_canvas_tile_photo), item.resolvedContentScale.toContentScale())
    }
}

@Composable
private fun BoxScope.LastPlayedTile(item: CanvasItem.LastPlayedItem, recent: List<JarnCanvasRom>) {
    val shape = RoundedCornerShape(item.resolvedBackgroundCornerRadiusDp.dp)
    val background = item.backgroundColorArgb?.let { Color(it) } ?: Color.Transparent
    val games = recent.take(item.resolvedCount)
    Column(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .background(background)
            .padding(JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        JarnText(
            text = stringResource(R.string.jarnfe_canvas_tile_last_played),
            style = JarnTheme.type.captionStrong,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
        games.forEach { game -> LastPlayedRow(game, item.resolvedContentScale.toContentScale()) }
    }
}

@Composable
private fun LastPlayedRow(game: JarnCanvasRom, scale: ContentScale) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(FolderIconSize)
                .clip(JarnTheme.radius.smShape),
        ) { CanvasImage(game.artPath, game.title, scale) }
        JarnText(game.title, JarnTheme.type.caption, JarnTheme.colors.textHigh, maxLines = 1)
    }
}
