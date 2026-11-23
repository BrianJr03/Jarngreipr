package jr.brian.jarnfe.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val WALL_SETTLE_MILLIS = 150L
private const val WALL_OVERLAY_ALPHA = 0.5f

/**
 * A tilted wall of a system's covers, drawn once at display size and cached. Shows nothing with
 * fewer than [LIBRARY_WALL_MIN_COVERS] readable covers, so the wallpaper behind it stays visible.
 * The previous wall holds until the next is ready, and no art is decoded while a game is up.
 */
@Composable
internal fun LibraryCoverWall(
    systemId: String,
    covers: List<LibraryWallCover>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val colors = JarnTheme.colors
    val palette = remember(colors) { colors.toWallPalette() }
    val selection = remember(covers) { libraryCoverWallSelection(covers) }
    val canDecode = rememberUpdatedState(rememberJarnArtCanLoad())
    var displaySize by remember { mutableStateOf(IntSize.Zero) }
    var shown by remember { mutableStateOf<ImageBitmap?>(null) }
    val wallKey = remember(systemId, selection, displaySize, palette) {
        libraryCoverWallKey(systemId, selection, displaySize.width, displaySize.height, palette)
    }
    LaunchedEffect(wallKey) {
        val key = wallKey ?: run {
            if (selection.isEmpty()) shown = null
            return@LaunchedEffect
        }
        LibraryCoverWallCache.attach(context)
        LibraryCoverWallCache[key]?.let { cached ->
            shown = cached
            return@LaunchedEffect
        }
        if (LibraryCoverWallCache.isUnavailable(key)) {
            shown = null
            return@LaunchedEffect
        }
        snapshotFlow { canDecode.value }.collectLatest { allowed ->
            if (!allowed) {
                LibraryCoverWallCache.keepOnly(key)
                return@collectLatest
            }
            val ready = LibraryCoverWallCache[key] ?: run {
                if (LibraryCoverWallCache.isUnavailable(key)) return@run null
                delay(WALL_SETTLE_MILLIS)
                renderLibraryCoverWall(context, key).also { LibraryCoverWallCache.store(key, it) }
            }
            shown = ready
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { displaySize = it }
            .clearAndSetSemantics {},
    ) {
        shown?.let { wall ->
            Image(
                bitmap = wall,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun JarnColors.toWallPalette() = LibraryCoverWallPalette(
    base = bg.toArgb(),
    card = raised.toArgb(),
    text = textHigh.toArgb(),
    overlay = bg.copy(alpha = WALL_OVERLAY_ALPHA).toArgb(),
)
