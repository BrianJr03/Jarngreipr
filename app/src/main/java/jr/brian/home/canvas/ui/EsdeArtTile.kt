package jr.brian.home.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.util.LocalEsdeWallpaperState
import jr.brian.home.esde.util.imagePathFor
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import java.io.File

/**
 * Renders the chosen [GameImageType] for the currently-focused ES-DE state.
 *
 * Resolution order (first non-null wins):
 *   1. **Game art** — `currentGame.imagePathFor(imageType)` when a game is
 *      focused. This is the per-tile, per-type lookup driven by §B of the
 *      "selectable image types" brief.
 *   2. **System art** — when no game is focused (browsing systems / no
 *      `currentGame`), fall back to the system art the wallpaper is already
 *      computing: `logoPath` for [GameImageType.Marquee] tiles, otherwise
 *      `currentImagePath`. Systems carry only one logo + one background in
 *      ES-DE, so every non-marquee tile shows the same system background —
 *      that is the intended result.
 *   3. **Gradient** — only when both above are null (cold boot before any
 *      ES-DE event, or a system/game with no art). Themed gradient `Box`
 *      with the focused game's name (or system name) centered in white.
 *      No AsyncImage in this branch — never a null model passed to Coil
 *      (which would render as a black image).
 *
 * Recomposes live as ES-DE events change either `currentGame` or the
 * system-level paths on `wallpaperState`.
 *
 * Scaling follows [EsdeContentScale]: `FIT` renders with `ContentScale.Fit`
 * + 8dp padding (logos centered on a transparent inner area); `CROP` uses
 * `ContentScale.Crop` with no padding. Tiles without a stored choice default
 * to `Marquee → FIT, others → CROP` — matching the original hardcoded rule.
 * Tap behavior mirrors `BaseTile`: plain `clickable` in edit mode so the outer slot's
 * long-press drag detector fires; `combinedClickable` outside edit mode so
 * long-press still opens the remove dialog. The card background is the
 * standard 0.85-alpha OledCardColor.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EsdeArtTile(
    resolved: ResolvedCanvasItem.EsdeArt,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    val state = LocalEsdeWallpaperState.current
    val imageType = resolved.raw.resolvedImageType
    val contentScale = resolved.raw.resolvedContentScale

    val path: String? = state.currentGame?.imagePathFor(imageType)
        ?: when (imageType) {
            GameImageType.Marquee -> state.logoPath
            else -> state.currentImagePath
        }

    val imageData = remember(path) { path?.let { toCoilModel(it) } }

    val clickModifier = if (editMode) {
        Modifier.clickable(onClick = onTap)
    } else {
        Modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress
        )
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .then(
                if (imageData == null) {
                    Modifier.background(brush = emptyArtBrush())
                } else {
                    Modifier
                }
            )
            .then(
                if (editMode) {
                    Modifier.border(
                        width = 3.dp,
                        color = ThemePrimaryColor,
                        shape = shape
                    )
                } else Modifier
            )
            .then(clickModifier)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        if (imageData != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageData).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = stringResource(R.string.canvas_esde_art_tile_description),
                contentScale = when (contentScale) {
                    EsdeContentScale.FIT -> ContentScale.Fit
                    EsdeContentScale.CROP -> ContentScale.Crop
                },
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (contentScale == EsdeContentScale.FIT) 8.dp else 0.dp)
            )
        } else {
            // Gradient empty state — overlay the focused entity's name so the
            // tile self-identifies even when no art exists. Game name when a
            // game is focused, system name when browsing systems, nothing at
            // cold boot.
            val title = state.currentGame?.name ?: state.currentSystemName
            Text(
                text = title ?: stringResource(R.string.settings_wallpaper_esde),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )

        }
    }
}

/**
 * Match `ESDEWallpaperContainer`'s model handling so file paths render the
 * same way the wallpaper does (raw filesystem paths must be wrapped in
 * `File(...)` — handing Coil the raw string can yield a black load).
 */
private fun toCoilModel(path: String): Any = when {
    path.isBlank() -> path
    path.startsWith("file:///android_asset/") -> path
    path.startsWith("content://") -> path.toUri()
    path.startsWith("http://") || path.startsWith("https://") -> path
    else -> File(path)
}

@Composable
private fun emptyArtBrush(): Brush = Brush.linearGradient(
    colors = listOf(
        ThemePrimaryColor.copy(alpha = 0.55f),
        ThemeAccentColor.copy(alpha = 0.45f),
        ThemeSecondaryColor.copy(alpha = 0.65f)
    )
)
