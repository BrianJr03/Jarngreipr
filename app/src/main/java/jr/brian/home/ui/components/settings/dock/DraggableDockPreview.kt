package jr.brian.home.ui.components.settings.dock

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.DockSize
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableDockPreview(
    apps: List<AppInfo>,
    dockColor: Color,
    dockSize: DockSize
) {
    val dockManager = LocalDockManager.current
    val customIconManager = LocalCustomIconManager.current
    val dockPackageNames by dockManager.dockApps.collectAsStateWithLifecycle()

    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    
    val itemPositions = remember { mutableMapOf<Int, Pair<Offset, Float>>() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = dockColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = dockSize.padding, vertical = dockSize.padding)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockPackageNames.forEachIndexed { index, packageName ->
                    if (index > 0) {
                        Spacer(Modifier.width(dockSize.spacing))
                    }
                    
                    val app = if (packageName.isNotEmpty()) {
                        apps.find { it.packageName == packageName }
                    } else null
                    
                    DraggableDockItem(
                        app = app,
                        index = index,
                        dockSize = dockSize,
                        isDragging = draggedIndex == index,
                        isTarget = targetIndex == index && draggedIndex != -1 && draggedIndex != index,
                        customIconManager = customIconManager,
                        onDragStart = { 
                            draggedIndex = index
                            dragOffset = Offset.Zero
                        },
                        onDrag = { offset ->
                            dragOffset = offset
                            
                            // Calculate which item we're hovering over
                            val currentCenter = (itemPositions[draggedIndex]?.first ?: Offset.Zero) + offset
                            var closestIndex = -1
                            var closestDistance = Float.MAX_VALUE
                            
                            itemPositions.forEach { (itemIndex, posAndWidth) ->
                                if (itemIndex != draggedIndex) {
                                    val (itemPos, itemWidth) = posAndWidth
                                    val itemCenter = itemPos + Offset(itemWidth / 2, 0f)
                                    val distance = (currentCenter - itemCenter).getDistance()
                                    
                                    if (distance < closestDistance && distance < itemWidth * 1.5f) {
                                        closestDistance = distance
                                        closestIndex = itemIndex
                                    }
                                }
                            }
                            
                            targetIndex = closestIndex
                        },
                        onDragEnd = {
                            if (targetIndex != -1 && draggedIndex != -1 && targetIndex != draggedIndex) {
                                dockManager.swapDockApps(draggedIndex, targetIndex)
                            }
                            draggedIndex = -1
                            dragOffset = Offset.Zero
                            targetIndex = -1
                        },
                        onPositioned = { position, width ->
                            itemPositions[index] = position to width
                        }
                    )
                }
            }
        }
        
        if (draggedIndex != -1) {
            val draggedPackageName = dockPackageNames.getOrNull(draggedIndex)
            val draggedApp = draggedPackageName?.let { pkgName ->
                if (pkgName.isNotEmpty()) apps.find { it.packageName == pkgName } else null
            }
            
            val startPosition = itemPositions[draggedIndex]?.first ?: Offset.Zero
            
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (startPosition.x + dragOffset.x).roundToInt(),
                            (startPosition.y + dragOffset.y).roundToInt()
                        )
                    }
                    .size(dockSize.containerSize)
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                        shadowElevation = 8.dp.toPx()
                    }
                    .zIndex(10f)
            ) {
                if (draggedApp != null) {
                    AppIconImage(
                        defaultIcon = draggedApp.icon,
                        packageName = draggedApp.packageName,
                        contentDescription = draggedApp.label,
                        customIconManager = customIconManager,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(dockSize.iconSize)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(dockSize.containerSize)
                            .background(
                                color = OledCardColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}
