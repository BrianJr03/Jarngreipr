package jr.brian.home.ui.components.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.DockSize
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalDockManager

@Composable
fun SwappableDockPreview(
    apps: List<AppInfo>,
    dockColor: Color,
    dockSize: DockSize
) {
    val dockManager = LocalDockManager.current
    val dockPackageNames by dockManager.dockApps.collectAsStateWithLifecycle()

    var swapModeEnabled by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    fun handleAppClick(index: Int) {
        val currentDockApps = dockPackageNames
        if (currentDockApps[index].isEmpty()) {
            if (swapModeEnabled && selectedIndex == index) {
                swapModeEnabled = false
                selectedIndex = -1
            } else {
                selectedIndex = index
            }
            return
        }

        if (swapModeEnabled) {
            if (selectedIndex == index) {
                swapModeEnabled = false
                selectedIndex = -1
            } else {
                dockManager.swapDockApps(selectedIndex, index)
                swapModeEnabled = false
                selectedIndex = -1
            }
        } else {
            selectedIndex = index
            swapModeEnabled = true
        }
    }

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

                    val isSelected = selectedIndex == index && swapModeEnabled

                    SwappableDockItem(
                        app = app,
                        dockSize = dockSize,
                        isSelected = isSelected,
                        swapModeEnabled = swapModeEnabled,
                        onClick = { handleAppClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwappableDockItem(
    app: AppInfo?,
    dockSize: DockSize,
    isSelected: Boolean,
    swapModeEnabled: Boolean,
    onClick: () -> Unit
) {
    val customIconManager = LocalCustomIconManager.current
    Card(
        onClick = onClick,
        modifier = Modifier.size(dockSize.containerSize),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color.Gray.copy(alpha = 0.3f)
                swapModeEnabled -> ThemePrimaryColor.copy(alpha = 0.15f)
                else -> Color.Transparent
            }
        ),
        border = when {
            isSelected -> null
            swapModeEnabled -> androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = ThemePrimaryColor
            )
            else -> null
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (app != null) {
                AppIconImage(
                    defaultIcon = app.icon,
                    packageName = app.packageName,
                    contentDescription = app.label,
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