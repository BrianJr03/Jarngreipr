package jr.brian.home.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.content.edit
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.components.dialog.ThemeShareInfoDialog
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.settings.PingAutoStartToggleItem
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.components.settings.WallpaperNearbyAutoStartToggleItem
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.viewmodels.ThemeShareViewModel
import jr.brian.ping.PingPermissions.hasPingPermissions
import jr.brian.pingnearby.PingNearbyPermissions.hasNearbyPermissions

@Composable
fun ThemeShareScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThemeShareViewModel = hiltViewModel()
) {
    val receivedThemes by viewModel.receivedThemes.collectAsStateWithLifecycle()
    val receivedWallpapers by viewModel.receivedWallpapers.collectAsStateWithLifecycle()
    val themeManager = LocalThemeManager.current
    val wallpaperManager = LocalWallpaperManager.current
    val discoveredEndpoints by themeManager.nearbyDiscoveredEndpoints.collectAsStateWithLifecycle()
    val connectedEndpoints by themeManager.connectedEndpoints.collectAsStateWithLifecycle()
    val isDiscoveringWallpaper = themeManager.isWallpaperNearbyRunning
    val context = LocalContext.current

    val infoDialogState = rememberDialogState<Unit>()

    var isPinging by remember { mutableStateOf(themeManager.isPingAutoStart) }
    var showNameKeyboard by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
    var focusedKeyIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("gaming_launcher_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("theme_share_info_seen", false)) {
            infoDialogState.show()
        }
    }

    LaunchedEffect(themeManager.isPingAutoStart) {
        if (themeManager.isPingAutoStart && !isPinging) {
            if (context.hasPingPermissions()) {
                themeManager.shareCurrentTheme()
                isPinging = true
            }
        } else if (!themeManager.isPingAutoStart && isPinging) {
            themeManager.stopSharing()
            isPinging = false
        }
    }

    LaunchedEffect(themeManager.isWallpaperNearbyAutoStart) {
        if (themeManager.isWallpaperNearbyAutoStart && !isDiscoveringWallpaper) {
            if (context.hasNearbyPermissions()) {
                themeManager.startWallpaperNearby()
            }
        } else if (!themeManager.isWallpaperNearbyAutoStart && isDiscoveringWallpaper) {
            themeManager.stopWallpaperNearby()
        }
    }

    LaunchedEffect(themeManager) {
        themeManager.receivedWallpaper.collect { bitmap ->
            viewModel.saveReceivedWallpaper(bitmap)
        }
    }

    if (infoDialogState.isVisible) {
        ThemeShareInfoDialog(
            onDismiss = {
                val prefs =
                    context.getSharedPreferences("gaming_launcher_prefs", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("theme_share_info_seen", true) }
                infoDialogState.dismiss()
            }
        )
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ScreenHeader(onBackClick = onNavigateBack) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.theme_sharing_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.theme_sharing_subtitle),
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPinging) stringResource(R.string.theme_sharing_stop)
                        else stringResource(R.string.theme_sharing_ping),
                        color = if (isPinging) Color.Red.copy(alpha = 0.8f) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (context.hasPingPermissions()) {
                                if (isPinging) {
                                    themeManager.stopSharing()
                                } else {
                                    themeManager.shareCurrentTheme()
                                }
                                isPinging = !isPinging
                            }
                        }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OledCardColor, RoundedCornerShape(8.dp))
                            .clickable { showNameKeyboard = !showNameKeyboard }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = themeManager.pingDisplayName.ifBlank { Build.MODEL },
                            color = if (themeManager.pingDisplayName.isBlank()) Color.Gray
                            else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.theme_sharing_edit_name_description),
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = themeManager.pingDisplayName.isBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.theme_sharing_name_default_hint,
                                Build.MODEL
                            ),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showNameKeyboard,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            QwertyKeyboard(
                                searchQuery = themeManager.pingDisplayName,
                                showQueryText = false,
                                showFlipLayoutButton = false,
                                onQueryChange = { themeManager.updatePingDisplayName(it) },
                                keyboardFocusRequesters = keyboardFocusRequesters,
                                onFocusChanged = { focusedKeyIndex = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    PingAutoStartToggleItem()
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    WallpaperNearbyAutoStartToggleItem()
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .background(OledCardColor, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nearby_wallpaper_subtitle),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    val currentUri = themeManager.getCurrentWallpaperUri()
                    if (currentUri != null) {
                        val isEsde = themeManager.isEsdeModeActive()
                        Text(
                            text = if (isEsde)
                                stringResource(R.string.nearby_wallpaper_current_esde)
                            else
                                stringResource(R.string.nearby_wallpaper_current_path),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.nearby_wallpaper_no_wallpaper),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = if (isDiscoveringWallpaper)
                            stringResource(R.string.nearby_wallpaper_stop)
                        else
                            stringResource(R.string.nearby_wallpaper_start),
                        color = if (isDiscoveringWallpaper) Color.Red.copy(alpha = 0.8f) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (isDiscoveringWallpaper) themeManager.stopWallpaperNearby()
                            else themeManager.startWallpaperNearby()
                        }
                    )
                }
            }

            if (isDiscoveringWallpaper) {
                if (discoveredEndpoints.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.nearby_wallpaper_scanning),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                } else {
                    discoveredEndpoints.forEach { (endpointId, deviceName) ->
                        item {
                            val isConnected = endpointId in connectedEndpoints
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 2.dp)
                                    .background(OledCardColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = deviceName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        text = if (isConnected) "● Connected" else "○ Discovered",
                                        color = if (isConnected) Color.Green else Color.Gray,
                                        fontSize = 11.sp,
                                    )
                                }
                                val hasWallpaper = themeManager.getCurrentWallpaperUri() != null
                                Text(
                                    text = stringResource(R.string.nearby_wallpaper_send),
                                    color = if (hasWallpaper && isConnected) ThemeSecondaryColor else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable(enabled = hasWallpaper && isConnected) {
                                        themeManager.sendWallpaperTo(endpointId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (receivedWallpapers.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.nearby_wallpaper_received_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                receivedWallpapers.forEach { (key, uriString) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .background(OledCardColor, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = uriString,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                            )
                            Text(
                                text = stringResource(R.string.nearby_wallpaper_apply),
                                color = ThemeSecondaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        wallpaperManager.setWallpaper(uriString, WallpaperType.IMAGE)
                                        wallpaperManager.updateSavedImageUri(uriString)
                                        Toast.makeText(context, "Applied Wallpaper", Toast.LENGTH_SHORT).show()
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Red, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = { viewModel.deleteReceivedWallpaper(key) },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.nearby_wallpaper_delete_description),
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (receivedThemes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.theme_sharing_empty),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                receivedThemes.forEach { (displayName, themes) ->
                    item {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            items(themes) { theme ->
                                ReceivedThemeCard(
                                    theme = theme,
                                    onClick = {
                                        themeManager.addCustomTheme(theme)
                                        themeManager.setTheme(theme)
                                        Toast.makeText(context, "Applied Theme", Toast.LENGTH_SHORT).show()
                                    },
                                    onDelete = {
                                        viewModel.deleteSharedTheme(displayName, theme.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedThemeCard(
    theme: ColorTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    val gradient = if (theme.isSolid) {
        Brush.linearGradient(listOf(theme.primaryColor, theme.primaryColor))
    } else {
        Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor))
    }

    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = modifier
                .offset(y = pressOffsetY)
                .scale(pressScale)
                .pressWithHaptic(
                    onClick,
                    haptic = haptic,
                    onPressChange = { isPressed = it }
                )
                .width(120.dp)
                .height(80.dp)
                .scale(animatedFocusedScale(isFocused))
                .background(brush = gradient, shape = RoundedCornerShape(12.dp))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 8.dp, y = (-8).dp)
                .size(24.dp)
                .background(color = Color.Red, shape = CircleShape)
                .border(width = 2.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .clickable(
                    onClick = onDelete,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.custom_theme_delete),
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
