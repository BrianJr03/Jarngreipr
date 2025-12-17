package jr.brian.home.ui.components.folder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.model.AppFolder
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIcon(
    folder: AppFolder,
    apps: List<AppInfo>,
    keyboardVisible: Boolean,
    offsetX: Float,
    offsetY: Float,
    onOffsetChanged: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isDraggingEnabled: Boolean = true,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragOver: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }
    var isDraggedOver by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isDraggedOver) 1.15f else if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "folderScale"
    )

    val folderApps = apps.filter { it.packageName in folder.apps }.take(4)

    Box(
        modifier = modifier
            .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(folder.iconSize.dp)
                    .scale(scale)
                    .then(
                        if (isDraggingEnabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = {
                                        onDragEnd()
                                        isDraggedOver = false
                                    },
                                    onDragCancel = {
                                        onDragEnd()
                                        isDraggedOver = false
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    currentOffsetX += dragAmount.x
                                    currentOffsetY += dragAmount.y
                                    onOffsetChanged(currentOffsetX, currentOffsetY)
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemePrimaryColor.copy(alpha = 0.3f))
                    .border(
                        width = 2.dp,
                        color = if (isDraggedOver) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
            ) {
                if (folderApps.isNotEmpty()) {
                    FolderPreviewGrid(
                        apps = folderApps,
                        iconSize = folder.iconSize
                    )
                }
            }

            if (!keyboardVisible) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontSize = (folder.iconSize / 6).sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                val dividerAlpha by animateFloatAsState(
                    targetValue = if (isFocused) 1f else 0f,
                    label = "dividerAlpha"
                )

                HorizontalDivider(
                    color = ThemePrimaryColor,
                    thickness = 4.dp,
                    modifier = Modifier.alpha(dividerAlpha)
                )
            }
        }
    }
}

@Composable
private fun FolderPreviewGrid(
    apps: List<AppInfo>,
    iconSize: Float
) {
    val previewSize = (iconSize / 2.5f).dp

    Box(
        modifier = Modifier
            .size(iconSize.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (apps.size) {
            1 -> {
                Image(
                    painter = rememberAsyncImagePainter(model = apps[0].icon),
                    contentDescription = null,
                    modifier = Modifier.size(previewSize * 1.5f)
                )
            }

            2 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = apps[0].icon),
                        contentDescription = null,
                        modifier = Modifier.size(previewSize)
                    )
                    Spacer(Modifier.height(4.dp))
                    Image(
                        painter = rememberAsyncImagePainter(model = apps[1].icon),
                        contentDescription = null,
                        modifier = Modifier.size(previewSize)
                    )
                }
            }

            3 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = apps[0].icon),
                        contentDescription = null,
                        modifier = Modifier.size(previewSize)
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Row {
                        Image(
                            painter = rememberAsyncImagePainter(model = apps[1].icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                        Spacer(Modifier.size(4.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = apps[2].icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    androidx.compose.foundation.layout.Row {
                        Image(
                            painter = rememberAsyncImagePainter(model = apps[0].icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                        Spacer(Modifier.size(4.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = apps[1].icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.foundation.layout.Row {
                        Image(
                            painter = rememberAsyncImagePainter(model = apps[2].icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                        Spacer(Modifier.size(4.dp))
                        Image(
                            painter = rememberAsyncImagePainter(model = apps.getOrNull(3)?.icon),
                            contentDescription = null,
                            modifier = Modifier.size(previewSize)
                        )
                    }
                }
            }
        }
    }
}
