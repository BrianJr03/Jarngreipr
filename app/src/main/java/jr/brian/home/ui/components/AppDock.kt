package jr.brian.home.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.DockManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedDockItemAlpha
import jr.brian.home.ui.animations.animatedDockItemScale
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.dockItemEnterAnimation
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalDockManager

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AppDock(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onEmptySlotClick: (Int) -> Unit,
    onEmptySlotLongClick: (Int) -> Unit,
    onAddSlotClick: (Int) -> Unit
) {
    val dockManager = LocalDockManager.current
    val dockPackageNames by dockManager.dockApps.collectAsState()

    val totalSlots = dockPackageNames.size
    val currentSlotCount = maxOf(1, totalSlots)
    val canAddMore = currentSlotCount < DockManager.MAX_DOCK_APPS

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until currentSlotCount) {
                    if (i > 0) {
                        Spacer(Modifier.width(8.dp))
                    }

                    val packageName = dockPackageNames.getOrNull(i)?.takeIf { it.isNotEmpty() }
                    val app = packageName?.let { pkgName ->
                        apps.find { it.packageName == pkgName }
                    }

                    if (app != null) {
                        DockAppItem(
                            app = app,
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                    } else {
                        DockEmptySlot(
                            onClick = { onEmptySlotClick(i) },
                            onLongClick = { onEmptySlotLongClick(i) }
                        )
                    }
                }

                if (canAddMore) {
                    Spacer(Modifier.width(8.dp))
                    DockAddButton(
                        onClick = { onAddSlotClick(currentSlotCount) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DockAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val enterScale = animatedDockItemScale()
    val enterAlpha = animatedDockItemAlpha()

    Box(
        modifier = Modifier
            .size(56.dp)
            .dockItemEnterAnimation(
                scale = enterScale,
                alpha = enterAlpha
            )
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DockEmptySlot(
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val enterScale = animatedDockItemScale()
    val enterAlpha = animatedDockItemAlpha()

    Box(
        modifier = Modifier
            .size(56.dp)
            .dockItemEnterAnimation(
                scale = enterScale,
                alpha = enterAlpha
            )
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = OledCardColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
private fun DockAddButton(
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val enterScale = animatedDockItemScale()
    val enterAlpha = animatedDockItemAlpha()

    Box(
        modifier = Modifier
            .size(56.dp)
            .dockItemEnterAnimation(
                scale = enterScale,
                alpha = enterAlpha
            )
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = OledCardColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (isFocused) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.dock_add_app),
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
    }
}
