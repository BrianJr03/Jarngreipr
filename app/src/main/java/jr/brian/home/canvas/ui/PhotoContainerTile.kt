package jr.brian.home.canvas.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import jr.brian.home.R
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import java.io.File

/**
 * User-configurable photo tile. Shows a themed "Tap to configure" placeholder
 * until the item's [imageUri] is set; once set, renders the image via the
 * app-wide default Coil `ImageLoader` (built as an `ImageLoaderFactory` in
 * [jr.brian.home.JarngreiprApplication]) so animated GIF / animated WebP
 * animate without extra wiring.
 *
 * The image scales with the cell — `Modifier.fillMaxSize()` lets Coil decode
 * at the laid-out size, so a resized-larger tile isn't just an upscaled
 * thumbnail. Fit vs Crop follows [CanvasItem.PhotoContainer.resolvedContentScale];
 * a decode error falls back to the placeholder so a revoked / missing URI
 * doesn't leave a broken tile behind.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoContainerTile(
    resolved: ResolvedCanvasItem.PhotoContainer,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
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
            .then(clickModifier)
            .focusable()
            .padding(4.dp)
    ) {
        val uri = resolved.raw.imageUri
        // Reset the failure latch whenever the URI itself changes so a fresh
        // pick isn't stuck on the previous URI's error state.
        var loadFailed by remember(uri) { mutableStateOf(false) }

        if (uri == null || loadFailed) {
            PhotoContainerPlaceholder(editMode = editMode)
        } else {
            PhotoContainerImage(
                uri = uri,
                contentScale = resolved.raw.resolvedContentScale,
                editMode = editMode,
                onLoadFailed = { loadFailed = true }
            )
        }
    }
}

@Composable
private fun PhotoContainerImage(
    uri: String,
    contentScale: EsdeContentScale,
    editMode: Boolean,
    onLoadFailed: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(toCoilModel(uri))
            .crossfade(true)
            // High-quality display: force ARGB_8888 (no RGB_565 fallback),
            // decode at the tile's exact laid-out pixel size (no downsampling
            // rounding), and keep hardware bitmaps for smooth scaling. Coil
            // still resizes to the actual layout size, so bigger tiles get
            // decoded at higher resolution automatically.
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowRgb565(false)
            .allowHardware(true)
            .precision(Precision.EXACT)
            .build(),
        contentDescription = stringResource(R.string.canvas_photo_container_description),
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .then(
                if (editMode) Modifier.border(
                    width = 3.dp,
                    color = ThemePrimaryColor,
                    shape = shape
                ) else Modifier
            ),
        contentScale = when (contentScale) {
            EsdeContentScale.FIT -> ContentScale.Fit
            EsdeContentScale.CROP -> ContentScale.Crop
        },
        filterQuality = FilterQuality.High,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) onLoadFailed()
        }
    )
}

/**
 * Coil accepts raw content URIs and http(s) URLs directly but chokes on bare
 * `file://` strings on some Android versions — wrap those in `File(...)` so the
 * decoder resolves the path itself. Mirrors [FrontendTile]'s Coil-model logic
 * so the two tile types agree on how they hand off paths to the loader.
 */
private fun toCoilModel(uri: String): Any = when {
    uri.isBlank() -> uri
    uri.startsWith("content://") -> uri.toUri()
    uri.startsWith("http://") || uri.startsWith("https://") -> uri
    uri.startsWith("file://") -> File(Uri.parse(uri).path ?: uri.removePrefix("file://"))
    else -> File(uri)
}

@Composable
private fun PhotoContainerPlaceholder(editMode: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(OledCardColor.copy(alpha = 0.7f))
            .border(
                width = if (editMode) 3.dp else 2.dp,
                color = ThemePrimaryColor.copy(alpha = if (editMode) 0.9f else 0.4f),
                shape = shape
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = ThemePrimaryColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.canvas_photo_container_tap_to_configure),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
