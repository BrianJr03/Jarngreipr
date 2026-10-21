package jr.brian.home.esde.ui.frontend

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.model.gridColumnsForScale
import jr.brian.home.esde.util.ESDEMediaConstants
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor

internal data class SystemTile(val systemName: String)

private const val NUM_COLS = 4

/**
 * Inset applied inside every cell so the focused-tile scale animation stays within
 * the cell slot — the lazy grid clips at cell bounds.
 */
private val TILE_INSET = FrontendTokens.Spacing.S

private val FOCUS_LIFT = FrontendTokens.Spacing.M

@Composable
internal fun SystemGrid(
    systems: List<SystemTile>,
    isLoading: Boolean,
    layout: FrontendLayout,
    modifier: Modifier = Modifier,
    initialRealIndex: Int = 0,
    focusResetKey: Any? = null,
    backgroundTransparent: Boolean = false,
    customizations: Map<String, SystemCustomization> = emptyMap(),
    reorderingSystem: String? = null,
    scanTotal: Int = 0,
    onSystemFocused: (SystemTile) -> Unit = {},
    onSystemSelected: (SystemTile) -> Unit = {},
    onSystemLongPressed: (SystemTile) -> Unit = {}
) {
    val prefsManager = LocalESDEPreferencesManager.current
    val prefs by prefsManager.state.collectAsStateWithLifecycle()

    // Always apply .background() with a varying color rather than
    // conditionally including the modifier in the chain. A chain-shape
    // change invalidates layout for the whole Box and cascades into the
    // FocusableTileLayout below, which briefly loses focus and jitters
    // the system cards while the user is picking a new frontend
    // wallpaper (backgroundTransparent flips mid-pick).
    Box(
        modifier = modifier.background(
            if (backgroundTransparent) Color.Transparent else OledBackgroundColor
        )
    ) {
        when {
            // While the scan is in flight and no systems have been published
            // yet, paint a skeleton grid in the same layout as the real tiles
            // so the UI shows structure rather than an empty screen. Once any
            // system streams in we fall through to the real grid branch.
            isLoading && systems.isEmpty() -> SystemSkeletonGrid(
                layout = layout,
                placeholderCount = skeletonPlaceholderCount(scanTotal),
                tileScale = prefs.frontendSystemTileScale,
                modifier = Modifier.fillMaxSize()
            )

            systems.isEmpty() -> Text(
                text = stringResource(R.string.frontend_no_systems),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
            )

            else -> {
                val systemKey by remember(systems) {
                    derivedStateOf { systems.joinToString("|") { it.systemName } }
                }
                val combinedFocusResetKey by remember(systemKey, focusResetKey) {
                    derivedStateOf {
                        if (focusResetKey == null) systemKey else "$systemKey|$focusResetKey"
                    }
                }
                val scale = prefs.frontendSystemTileScale
                // Force 2 columns on the external (bottom) display so the
                // smaller screen gets bigger system tiles. Keyed on the
                // physical assignment so a swap re-counts correctly.
                val onBottom = jr.brian.home.ui.util.rememberPhysicalAssignment() ==
                    jr.brian.core.model.ScreenAssignment.FRONTEND_ON_BOTTOM
                val columnCount = if (onBottom) 2 else gridColumnsForScale(scale)
                FocusableTileLayout(
                    items = systems,
                    layout = layout,
                    columns = columnCount,
                    contentPadding = PaddingValues(
                        horizontal = FrontendTokens.Spacing.L,
                        vertical = FrontendTokens.Spacing.XL
                    ),
                    // Tiles draw past their cell with clip=false while focused
                    // (scale-up + lift), so the auto-scroll has to reserve
                    // extra space per edge; otherwise a focused bottom-row
                    // tile sits flush against the viewport.
                    focusMargin = FOCUS_LIFT + FrontendTokens.Spacing.M,
                    rowItemWidth = DEFAULT_ROW_TILE_WIDTH * scale,
                    rowAlignment = prefs.frontendSystemRowAlignment,
                    initialRealIndex = initialRealIndex,
                    focusResetKey = combinedFocusResetKey,
                    onItemFocused = { tile -> tile?.let(onSystemFocused) },
                    itemKey = { _, tile -> tile.systemName }
                ) { _, tile, focusRequester, isFocused, onFocused ->
                    // Shrink each card a touch on the external (bottom)
                    // display. Keeps the 2-column grid but gives the
                    // smaller screen some breathing room instead of
                    // oversized tiles.
                    val tileInset = if (onBottom) 16.dp else 0.dp
                    Box(modifier = Modifier.padding(tileInset)) {
                        SystemTileCard(
                            tile = tile,
                            isFocused = isFocused,
                            focusRequester = focusRequester,
                            customization = customizations[tile.systemName],
                            isReordering = reorderingSystem == tile.systemName,
                            onFocused = onFocused,
                            onSelected = { onSystemSelected(tile) },
                            onLongPress = { onSystemLongPressed(tile) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemTileCard(
    tile: SystemTile,
    isFocused: Boolean,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    customization: SystemCustomization?,
    isReordering: Boolean,
    onFocused: () -> Unit,
    onSelected: () -> Unit,
    onLongPress: () -> Unit
) {
    val scale = animatedFocusedScale(isFocused)
    val shape = FrontendTokens.Radius.TileSmall
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(
            durationMillis = FrontendTokens.Motion.FocusMs,
            easing = FrontendTokens.Motion.Easing
        ),
        label = "systemTileFocus"
    )
    val floatPhase = focusFloatPhase(isFocused)
    val floatAmplitude = rememberFloatAmplitude()

    val backgroundColor = resolveBackgroundColor(customization)
    val view = LocalView.current
    val prefsManager = LocalESDEPreferencesManager.current
    val prefs by prefsManager.state.collectAsStateWithLifecycle()
    val hapticEnabled = prefs.frontendFocusHapticEnabled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                translationY =
                    (-FOCUS_LIFT.toPx() + floatAmplitude.toPx() * floatPhase) * focusProgress
                scaleX = scale
                scaleY = scale
                clip = false
            }
            .padding(TILE_INSET)
            .clip(shape)
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    if (hapticEnabled) view.emitFocusHapticIfReady()
                    onFocused()
                }
            }
            .onKeyEvent { event ->
                // Gamepad SELECT (back-of-the-pad "share" / "view" on many
                // controllers) opens system customization — same action as
                // long-press, surfaced on a dedicated button so users don't
                // have to find and hold the tile.
                if (event.type != KeyEventType.KeyUp) return@onKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                if (code == android.view.KeyEvent.KEYCODE_BUTTON_SELECT) {
                    onLongPress()
                    true
                } else false
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelected,
                onLongClick = onLongPress
            )
    ) {
        SystemTileContent(tile = tile, customization = customization)
        if (isReordering) {
            ReorderingHighlight()
        }
    }
}

@Composable
private fun resolveBackgroundColor(customization: SystemCustomization?): Color {
    val argb = customization?.solidColorArgb ?: return OledCardColor
    return Color(argb.toInt())
}

@Composable
private fun SystemTileContent(tile: SystemTile, customization: SystemCustomization?) {
    val customBackgroundUri = customization?.backgroundUri
    val showName = customization?.showName ?: true

    if (customBackgroundUri != null) {
        CustomBackgroundContent(
            systemName = tile.systemName,
            backgroundUri = customBackgroundUri,
            showName = showName
        )
    } else {
        DefaultSystemLogoContent(systemName = tile.systemName)
    }
}

@Composable
private fun CustomBackgroundContent(
    systemName: String,
    backgroundUri: String,
    showName: Boolean
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(backgroundUri).build(),
            contentDescription = systemName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (showName) {
            SystemNameOverlay(systemName = systemName)
        }
    }
}

@Composable
private fun SystemNameOverlay(systemName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = systemName.uppercase(),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(FrontendTokens.Spacing.S)
        )
    }
}

@Composable
private fun DefaultSystemLogoContent(systemName: String) {
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    val mediaName = remember(systemName) { getMediaSystemName(systemName) }
    val logoModel = remember(mediaName) {
        "${ESDEMediaConstants.SYSTEM_LOGOS_ASSET_PATH}/$mediaName.svg"
    }
    var hasError by remember(logoModel) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FrontendTokens.Spacing.S)
    ) {
        if (hasError) {
            SystemTileTextFallback(systemName = systemName)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(logoModel).build(),
                imageLoader = imageLoader,
                contentDescription = systemName,
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

@Composable
private fun ReorderingHighlight() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeAccentColor.copy(alpha = 0.18f))
    )
}

private const val DEFAULT_SKELETON_TILE_COUNT = 8

private fun skeletonPlaceholderCount(scanTotal: Int): Int =
    if (scanTotal > 0) scanTotal else DEFAULT_SKELETON_TILE_COUNT

@Composable
private fun SystemSkeletonGrid(
    layout: FrontendLayout,
    placeholderCount: Int,
    tileScale: Float,
    modifier: Modifier = Modifier
) {
    val count = placeholderCount.coerceAtLeast(1)
    val shimmerAlpha = rememberSkeletonShimmerAlpha()
    when (layout) {
        FrontendLayout.Grid -> LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumnsForScale(tileScale)),
            modifier = modifier,
            contentPadding = PaddingValues(
                horizontal = FrontendTokens.Spacing.L,
                vertical = FrontendTokens.Spacing.XL
            ),
            userScrollEnabled = false
        ) {
            items(count = count) {
                SystemSkeletonTile(shimmerAlpha = shimmerAlpha)
            }
        }

        FrontendLayout.Row -> LazyRow(
            modifier = modifier,
            contentPadding = PaddingValues(
                horizontal = FrontendTokens.Spacing.L,
                vertical = FrontendTokens.Spacing.XL
            ),
            horizontalArrangement = Arrangement.spacedBy(FrontendTokens.Spacing.S),
            userScrollEnabled = false
        ) {
            items(count = count) {
                Box(modifier = Modifier.width(DEFAULT_ROW_TILE_WIDTH * tileScale)) {
                    SystemSkeletonTile(shimmerAlpha = shimmerAlpha)
                }
            }
        }
    }
}

@Composable
private fun SystemSkeletonTile(shimmerAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(TILE_INSET)
            .clip(FrontendTokens.Radius.TileSmall)
            .background(OledCardColor.copy(alpha = shimmerAlpha))
    )
}

@Composable
private fun rememberSkeletonShimmerAlpha(): Float {
    val infinite = rememberInfiniteTransition(label = "systemSkeletonShimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "systemSkeletonAlpha"
    )
    return alpha
}
