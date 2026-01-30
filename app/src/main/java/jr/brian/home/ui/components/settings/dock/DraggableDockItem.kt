package jr.brian.home.ui.components.settings.dock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.CustomIconManager
import jr.brian.home.data.DockSize
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableDockItem(
    app: AppInfo?,
    index: Int,
    dockSize: DockSize,
    isDragging: Boolean,
    isTarget: Boolean,
    customIconManager: CustomIconManager,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onPositioned: (Offset, Float) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    
    LaunchedEffect(isTarget) {
        if (isTarget) {
            offsetX.animateTo(
                targetValue = if (index < 0) -10f else 10f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    
    Box(
        modifier = Modifier
            .size(dockSize.containerSize)
            .onGloballyPositioned { coordinates ->
                onPositioned(
                    coordinates.positionInParent(),
                    coordinates.size.width.toFloat()
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                alpha = if (isDragging) 0.3f else 1f
                scaleX = if (isTarget) 0.9f else 1f
                scaleY = if (isTarget) 0.9f else 1f
            }
            .pointerInput(index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            }
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(dockSize.iconSize)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(dockSize.containerSize)
                    .background(
                        color = OledCardColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}
