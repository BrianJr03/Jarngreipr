package jr.brian.home.canvas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeArtType
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.util.LocalEsdeWallpaperState
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

/**
 * Renders the currently-displayed ES-DE image (logo or background) for a
 * canvas tile. **Event-driven** — reads
 * [LocalEsdeWallpaperState] and recomposes whenever ES-DE events cause the
 * wallpaper's `logoPath` / `currentImagePath` to change.
 *
 * - Loads through [LocalESDEImageLoader] (shared cache, SVG / GIF support).
 * - LOGO: [ContentScale.Fit] preserves aspect, centered on a transparent
 *   background — matches the wallpaper's marquee handling.
 * - BACKGROUND: [ContentScale.Crop] fills the cell edge-to-edge.
 * - Null path (common at cold boot before any ES-DE event has fired) → a
 *   neutral in-cell placeholder. Never crash, never blank-that-looks-broken.
 * - In edit mode, draws the canvas-standard 3dp accent border so the user
 *   can see what they're about to drag/resize.
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
    val artType = resolved.raw.artType
    val path = when (artType) {
        EsdeArtType.LOGO -> state.logoPath
        EsdeArtType.BACKGROUND -> state.currentImagePath
    }

    // In edit mode the outer CanvasTileSlot's pointerInput owns long-press
    // for drag-to-move; using `combinedClickable` here would consume the
    // long-press locally and the drag gesture would never fire. Match
    // CanvasItemTile.BaseTile: plain `clickable` in edit, combinedClickable
    // outside (so the user can long-press to open the remove dialog).
    val clickModifier = if (editMode) {
        Modifier.clickable(onClick = onTap)
    } else {
        Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(OledCardColor.copy(alpha = 0.85f))
            .then(
                if (editMode) {
                    Modifier.border(
                        width = 3.dp,
                        color = ThemePrimaryColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .then(clickModifier)
            .focusable()
    ) {
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(path).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = stringResource(
                    when (artType) {
                        EsdeArtType.LOGO -> R.string.canvas_esde_art_logo_description
                        EsdeArtType.BACKGROUND -> R.string.canvas_esde_art_background_description
                    }
                ),
                contentScale = when (artType) {
                    EsdeArtType.LOGO -> ContentScale.Fit
                    EsdeArtType.BACKGROUND -> ContentScale.Crop
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (artType == EsdeArtType.LOGO) 8.dp else 0.dp)
            )
        } else {
            EsdeArtPlaceholder(artType = artType)
        }
    }
}

@Composable
private fun EsdeArtPlaceholder(artType: EsdeArtType) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = ThemePrimaryColor.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                when (artType) {
                    EsdeArtType.LOGO -> R.string.canvas_esde_art_waiting_logo
                    EsdeArtType.BACKGROUND -> R.string.canvas_esde_art_waiting_background
                }
            ),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
