package jr.brian.home.esde.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.components.settings.BackButton
import jr.brian.home.ui.extensions.combinedClickWithHaptic
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalIconShapeManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppPickerOverlay(
    allApps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(onBack = onDismiss)

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppGridItem(
                            app = app,
                            onAppClick = {
                                onAppSelected(app)
                            },
                            onAppLongClick = {}
                        )
                    }
                }

                BackButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledBackgroundColor)
            ) {
                val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
                var focusedKeyIndex by remember { mutableIntStateOf(0) }

                QwertyKeyboard(
                    searchQuery = searchQuery,
                    showFlipLayoutButton = false,
                    onQueryChange = { searchQuery = it },
                    keyboardFocusRequesters = keyboardFocusRequesters,
                    onFocusChanged = { focusedKeyIndex = it },
                    onFlipLayout = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit
) {
    val customIconManager = LocalCustomIconManager.current
    val iconShapeManager = LocalIconShapeManager.current
    val iconShape = iconShapeManager.iconShape.toComposeShape()
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickWithHaptic(
                haptic = haptic,
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
            .focusable()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(iconShape),
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = null,
                customIconManager = customIconManager,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(Modifier.height(4.dp))

        app.AppName()
    }
}
