package jr.brian.home.esde.ui.frontend

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.util.ESDEMediaConstants
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.runtime.derivedStateOf

internal data class SystemTile(val systemName: String)

private const val NUM_COLS = 4

/**
 * Inset applied inside every cell so the focused-tile scale animation stays within
 * the cell slot — the lazy grid clips at cell bounds.
 */
private val TILE_INSET = 12.dp

@Composable
internal fun SystemGrid(
    systems: List<SystemTile>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSystemFocused: (SystemTile) -> Unit = {},
    onSystemSelected: (SystemTile) -> Unit = {}
) {
    Box(modifier = modifier.background(OledBackgroundColor)) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = ThemeAccentColor
            )

            systems.isEmpty() -> Text(
                text = stringResource(R.string.frontend_no_systems),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )

            else -> SystemTileGrid(
                systems = systems,
                onSystemFocused = onSystemFocused,
                onSystemSelected = onSystemSelected
            )
        }
    }
}

@Composable
private fun SystemTileGrid(
    systems: List<SystemTile>,
    onSystemFocused: (SystemTile) -> Unit,
    onSystemSelected: (SystemTile) -> Unit
) {
    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var focusedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }

    LaunchedEffect(focusedIndex) {
        focusRequesters[focusedIndex]?.requestFocus()
    }

    val systemKey by remember(systems) {
        derivedStateOf { systems.joinToString("|") { it.systemName } }
    }
    LaunchedEffect(systemKey) {
        if (systems.isNotEmpty()) {
            focusedIndex = 0
            focusRequesters[0]?.requestFocus()
            onSystemFocused(systems[0])
        }
    }

    fun moveFocus(delta: Int) {
        val next = (focusedIndex + delta).coerceIn(0, systems.lastIndex)
        if (next == focusedIndex) return
        focusedIndex = next
        coroutineScope.launch {
            val layoutInfo = listState.layoutInfo
            val effectiveStart = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
            val effectiveEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == next }
            when {
                info == null -> listState.animateScrollToItem(next)
                info.offset.y < effectiveStart -> {
                    val px = effectiveStart - info.offset.y
                    listState.animateScrollBy(-px.toFloat())
                }
                info.offset.y + info.size.height > effectiveEnd -> {
                    val px = info.offset.y + info.size.height - effectiveEnd
                    listState.animateScrollBy(px.toFloat())
                }
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(NUM_COLS),
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { moveFocus(1); true }
                    KeyEvent.KEYCODE_DPAD_LEFT -> { moveFocus(-1); true }
                    KeyEvent.KEYCODE_DPAD_DOWN -> { moveFocus(NUM_COLS); true }
                    KeyEvent.KEYCODE_DPAD_UP -> { moveFocus(-NUM_COLS); true }
                    else -> false
                }
            },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(systems, key = { _, tile -> tile.systemName }) { index, tile ->
            val focusRequester = remember { FocusRequester() }
            DisposableEffect(index) {
                focusRequesters[index] = focusRequester
                onDispose { focusRequesters.remove(index) }
            }
            SystemTileCard(
                tile = tile,
                focusRequester = focusRequester,
                onFocused = {
                    if (focusedIndex != index) focusedIndex = index
                    onSystemFocused(tile)
                },
                onSelected = { onSystemSelected(tile) }
            )
        }
    }
}

@Composable
private fun SystemTileCard(
    tile: SystemTile,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onSelected: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale = animatedFocusedScale(isFocused)
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TILE_INSET),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(scale)
                .clip(shape)
                .background(OledCardColor)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clickable { onSelected() }
        ) {
            SystemTileContent(tile = tile)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .background(if (isFocused) ThemeAccentColor else Color.Transparent)
        )
    }
}

@Composable
private fun SystemTileContent(tile: SystemTile) {
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    val mediaName = remember(tile.systemName) { getMediaSystemName(tile.systemName) }
    val logoModel = remember(mediaName) {
        "${ESDEMediaConstants.SYSTEM_LOGOS_ASSET_PATH}/$mediaName.svg"
    }
    var hasError by remember(logoModel) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (hasError) {
            SystemTileTextFallback(systemName = tile.systemName)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(logoModel).build(),
                imageLoader = imageLoader,
                contentDescription = tile.systemName,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) hasError = true
                }
            )
        }
    }
}

@Composable
private fun SystemTileTextFallback(systemName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = systemName.uppercase(),
            color = ThemeAccentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium
        )
    }
}


