package jr.brian.home.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.AppVisibilityDialog
import jr.brian.home.ui.components.WallpaperOptionButton
import jr.brian.home.ui.theme.AppBackgroundDark
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.LocalGridSettingsManager
import jr.brian.home.ui.theme.LocalPowerSettingsManager
import jr.brian.home.ui.theme.LocalThemeManager
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.WallpaperType
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.WallpaperUtils
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SettingsScreen(
    allApps: List<jr.brian.home.model.AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackgroundDark,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .systemBarsPadding(),
        ) {
            Column {
                VersionInfo()
                SettingsContent(
                    allApps = allApps,
                    onNavigateToFAQ = onNavigateToFAQ
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    allApps: List<jr.brian.home.model.AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit = {}
) {
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }
    val wallpaperFocusRequester = remember { FocusRequester() }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val isThorDevice = remember {
        android.os.Build.MODEL == DeviceModel.THOR
    }

    LaunchedEffect(Unit) {
        delay(10)
        firstItemFocusRequester.requestFocus()
    }

    if (showAppVisibilityDialog) {
        AppVisibilityDialog(
            apps = allApps,
            onDismiss = { showAppVisibilityDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            ThemeSelectorItem(
                focusRequester = firstItemFocusRequester,
                isExpanded = expandedItem == "theme",
                onExpandChanged = { expandedItem = if (it) "theme" else null }
            )
        }

        item {
            WallpaperSelectorItem(
                focusRequester = wallpaperFocusRequester,
                isExpanded = expandedItem == "wallpaper",
                onExpandChanged = { expandedItem = if (it) "wallpaper" else null }
            )
        }

        item {
            GridColumnSelectorItem(
                isExpanded = expandedItem == "grid",
                onExpandChanged = { expandedItem = if (it) "grid" else null },
                totalAppsCount = allApps.size
            )
        }

        if (isThorDevice) {
            item {
                ThorSettingsItem(
                    isExpanded = expandedItem == "thor",
                    onExpandChanged = { expandedItem = if (it) "thor" else null }
                )
            }
        }

        item {
            SettingItem(
                title = stringResource(id = R.string.settings_app_visibility_title),
                description = stringResource(id = R.string.settings_app_visibility_description),
                icon = Icons.Default.Visibility,
                onClick = {
                    expandedItem = null
                    showAppVisibilityDialog = true
                },
            )
        }

        item {
            SettingItem(
                title = stringResource(id = R.string.settings_faq_title),
                description = stringResource(id = R.string.settings_faq_description),
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = {
                    expandedItem = null
                    onNavigateToFAQ()
                },
            )
        }

        item {
            val url = stringResource(R.string.settings_buy_me_coffee_url)
            SettingItem(
                title = stringResource(id = R.string.settings_buy_me_coffee_title),
                description = stringResource(id = R.string.settings_buy_me_coffee_description),
                icon = Icons.Default.Coffee,
                onClick = {
                    expandedItem = null
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        url.toUri()
                    )
                    context.startActivity(intent)
                },
            )
        }
    }
}

@Composable
private fun ThemeSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val themeManager = LocalThemeManager.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }
    val selectedThemeFocusRequesters =
        remember { ColorTheme.allThemes.associateWith { FocusRequester() } }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            val selectedTheme = themeManager.currentTheme
            selectedThemeFocusRequesters[selectedTheme]?.requestFocus()
        } else {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.settings_palette_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_color_theme_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_color_theme_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(ColorTheme.allThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = themeManager.currentTheme.id == theme.id,
                        onClick = {
                            themeManager.setTheme(theme)
                            onExpandChanged(false)
                        },
                        focusRequester = selectedThemeFocusRequesters[theme],
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    theme.primaryColor,
                    theme.secondaryColor,
                ),
        )

    val borderColor = when {
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        modifier =
            Modifier
                .width(120.dp)
                .height(80.dp)
                .scale(animatedFocusedScale(isFocused))
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = gradient,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = theme.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun WallpaperSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val wallpaperManager = LocalWallpaperManager.current
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val detectedType = WallpaperUtils.detectWallpaperType(context, it)
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
                wallpaperManager.setWallpaper(it.toString(), detectedType)
            } catch (_: SecurityException) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val extension = when (detectedType) {
                        WallpaperType.GIF -> "gif"
                        WallpaperType.VIDEO -> "mp4"
                        else -> "jpg"
                    }
                    val fileName = "wallpaper_${System.currentTimeMillis()}.$extension"
                    val outputFile = File(context.filesDir, fileName)
                    inputStream?.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    wallpaperManager.setWallpaper(
                        "file://${outputFile.absolutePath}",
                        detectedType
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        onExpandChanged(false)
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = stringResource(R.string.settings_wallpaper_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_wallpaper_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_wallpaper_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_default),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.NONE,
                    onClick = {
                        wallpaperManager.clearWallpaper()
                        onExpandChanged(false)
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_image_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.IMAGE,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("image/*"))
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_gif_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.GIF,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("image/gif"))
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_video_picker),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.VIDEO,
                    onClick = {
                        mediaPickerLauncher.launch(arrayOf("video/*"))
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_transparent),
                    isSelected = wallpaperManager.getWallpaperType() == WallpaperType.TRANSPARENT,
                    onClick = {
                        wallpaperManager.setTransparent()
                        onExpandChanged(false)
                    }
                )
            }
        }
    }
}


@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                )
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                .background(
                    brush = cardGradient,
                    shape = RoundedCornerShape(16.dp),
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    brush =
                        borderBrush(
                            isFocused = isFocused,
                            colors =
                                listOf(
                                    ThemePrimaryColor.copy(alpha = 0.8f),
                                    ThemeSecondaryColor.copy(alpha = 0.6f),
                                ),
                        ),
                    shape = RoundedCornerShape(16.dp),
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    onClick()
                }
                .focusable()
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.settings_coffee_icon_description),
                modifier =
                    Modifier
                        .size(32.dp)
                        .rotate(animatedRotation(isFocused)),
                tint = Color.White,
            )

            Spacer(modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = if (isFocused) 18.sp else 16.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun GridColumnSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    totalAppsCount: Int = 0
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }
    val columnsMinusFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            columnsMinusFocusRequester.requestFocus()
        } else {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.settings_grid_columns_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_grid_columns_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_grid_columns_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val gridCapacity = gridSettingsManager.columnCount * gridSettingsManager.rowCount
                val hiddenAppsCount = (totalAppsCount - gridCapacity).coerceAtLeast(0)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (hiddenAppsCount > 0) {
                                ThemeSecondaryColor.copy(alpha = 0.15f)
                            } else {
                                ThemePrimaryColor.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (hiddenAppsCount > 0) {
                                ThemeSecondaryColor.copy(alpha = 0.5f)
                            } else {
                                ThemePrimaryColor.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (hiddenAppsCount > 0) {
                            if (gridCapacity == 1 && hiddenAppsCount == 1) {
                                stringResource(
                                    R.string.settings_grid_apps_hidden_singular,
                                    gridCapacity,
                                    hiddenAppsCount
                                )
                            } else {
                                stringResource(
                                    R.string.settings_grid_apps_hidden_plural,
                                    gridCapacity,
                                    hiddenAppsCount
                                )
                            }
                        } else {
                            if (gridCapacity == 1) {
                                stringResource(
                                    R.string.settings_grid_all_apps_visible_singular,
                                    gridCapacity
                                )
                            } else {
                                stringResource(
                                    R.string.settings_grid_all_apps_visible_plural,
                                    gridCapacity
                                )
                            }
                        },
                        color = if (hiddenAppsCount > 0) {
                            ThemeSecondaryColor
                        } else {
                            ThemePrimaryColor
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                GridDimensionSelector(
                    label = stringResource(R.string.settings_grid_columns_label),
                    value = gridSettingsManager.columnCount,
                    minValue = GridSettingsManager.MIN_COLUMNS,
                    maxValue = GridSettingsManager.MAX_COLUMNS,
                    onValueChange = { newValue ->
                        gridSettingsManager.updateColumnCount(newValue)
                    },
                    minusFocusRequester = columnsMinusFocusRequester,
                )

                GridDimensionSelector(
                    label = stringResource(R.string.settings_grid_rows_label),
                    value = gridSettingsManager.rowCount,
                    minValue = GridSettingsManager.MIN_ROWS,
                    maxValue = GridSettingsManager.MAX_ROWS,
                    onValueChange = { newValue ->
                        gridSettingsManager.updateRowCount(newValue)
                    },
                )

                GridControlButton(
                    text = stringResource(R.string.settings_grid_done),
                    onClick = { onExpandChanged(false) },
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun GridDimensionSelector(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    minusFocusRequester: FocusRequester? = null,
    plusFocusRequester: FocusRequester? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GridControlButton(
                text = stringResource(R.string.settings_grid_minus),
                onClick = {
                    if (value > minValue) {
                        onValueChange(value - 1)
                    }
                },
                enabled = value > minValue,
                modifier = Modifier.weight(1f),
                focusRequester = minusFocusRequester
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = AppCardDark,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = ThemePrimaryColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            GridControlButton(
                text = stringResource(R.string.settings_grid_plus),
                onClick = {
                    if (value < maxValue) {
                        onValueChange(value + 1)
                    }
                },
                enabled = value < maxValue,
                modifier = Modifier.weight(1f),
                focusRequester = plusFocusRequester
            )
        }
    }
}

@Composable
private fun GridControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = when {
            !enabled -> listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
            isFocused -> listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.7f),
            )

            isPrimary -> listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.4f),
            )

            else -> listOf(
                AppCardLight,
                AppCardDark,
            )
        }
    )

    val borderColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        isFocused -> Color.White
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(if (isFocused) 1.05f else 1f)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = gradient,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = if (isPrimary) 18.sp else 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ThorSettingsItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {}
) {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.settings_thor_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_thor_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_thor_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThorSettingToggleButton(
                    text = stringResource(id = R.string.settings_thor_power_button),
                    isChecked = isPowerButtonVisible,
                    onClick = {
                        powerSettingsManager.setPowerButtonVisibility(!isPowerButtonVisible)
                        onExpandChanged(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ThorSettingToggleButton(
    text: String,
    isChecked: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.6f),
                    )
                } else {
                    listOf(
                        AppCardLight,
                        AppCardDark,
                    )
                },
        )

    val borderColor = when {
        isChecked -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isChecked || isFocused) 2.dp else 0.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                .background(
                    brush = gradient,
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .focusable()
                .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
            )

            Text(
                text = if (isChecked) "ON" else "OFF",
                color = if (isChecked) Color.Green else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun VersionInfo() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1"
    } catch (_: Exception) {
        "0.1"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 32.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = stringResource(R.string.settings_version_label, versionName),
            color = ThemeAccentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}