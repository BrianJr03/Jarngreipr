package jr.brian.home.ui.components.apps

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.resolveDisplayPath
import jr.brian.home.ui.extensions.combinedClickWithHaptic
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FreePositionedRomItem(
    rom: PinnedRomInfo,
    offsetX: Float,
    offsetY: Float,
    onOffsetChanged: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isDraggingEnabled: Boolean = true,
    iconSize: Float = ROM_DEFAULT_ICON_SIZE,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val appVisibilityManager = LocalAppVisibilityManager.current
    val haptic = LocalHapticFeedback.current
    val customIconManager = LocalCustomIconManager.current
    val customIconsMap by customIconManager.customIconsMap.collectAsStateWithLifecycle(initialValue = emptyMap())
    val customIconPath = customIconsMap[rom.key]
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }

    Box(modifier = Modifier.offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RomIconImage(
                rom = rom,
                iconSize = iconSize,
                customIconPath = customIconPath,
                isDraggingEnabled = isDraggingEnabled,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragDelta = { dx, dy ->
                    currentOffsetX += dx
                    currentOffsetY += dy
                    onOffsetChanged(currentOffsetX, currentOffsetY)
                },
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )

            Spacer(Modifier.height(4.dp))

            if (appVisibilityManager.showHomeScreenAppNames) {
                Text(
                    text = rom.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RomIconImage(
    rom: PinnedRomInfo,
    iconSize: Float,
    customIconPath: String?,
    isDraggingEnabled: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val artworkFile = customIconPath?.let { File(it).takeIf { f -> f.exists() } }
        ?: rom.resolveDisplayPath()?.let { File(it).takeIf { f -> f.exists() } }
    val iconSizePx = with(density) { iconSize.dp.roundToPx() }

    val updatedOnDragStart by rememberUpdatedState(onDragStart)
    val updatedOnDragEnd by rememberUpdatedState(onDragEnd)
    val updatedOnDragDelta by rememberUpdatedState(onDragDelta)

    Box(
        modifier = Modifier
            .size(iconSize.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isDraggingEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { updatedOnDragStart() },
                            onDragEnd = { updatedOnDragEnd() },
                            onDragCancel = { updatedOnDragEnd() }
                        ) { change, dragAmount ->
                            change.consume()
                            updatedOnDragDelta(dragAmount.x, dragAmount.y)
                        }
                    }
                } else Modifier
            )
            .combinedClickWithHaptic(
                haptic = haptic,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artworkFile != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkFile)
                    .crossfade(true)
                    .size(iconSizePx, iconSizePx)
                    .build(),
                contentDescription = stringResource(R.string.rom_search_icon_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(iconSize.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.VideogameAsset,
                contentDescription = stringResource(R.string.rom_search_icon_description),
                tint = ThemePrimaryColor,
                modifier = Modifier.size((iconSize * 0.55f).dp)
            )
        }
    }
}

const val ROM_DEFAULT_ICON_SIZE = 80f
