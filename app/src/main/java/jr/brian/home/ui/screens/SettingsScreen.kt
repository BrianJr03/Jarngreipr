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
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Palette
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
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.AppVisibilityDialog
import jr.brian.home.ui.theme.AppBackgroundDark
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.LocalThemeManager
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.WALLPAPER_TRANSPARENT
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SettingsScreen(
    allApps: List<jr.brian.home.model.AppInfo> = emptyList()
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
                SettingsContent(allApps = allApps)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    allApps: List<jr.brian.home.model.AppInfo> = emptyList()
) {
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }
    val wallpaperFocusRequester = remember { FocusRequester() }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }

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
            ThemeSelectorItem(focusRequester = firstItemFocusRequester)
        }

        item {
            WallpaperSelectorItem(focusRequester = wallpaperFocusRequester)
        }

        item {
            SettingItem(
                title = stringResource(id = R.string.settings_app_visibility_title),
                description = stringResource(id = R.string.settings_app_visibility_description),
                icon = Icons.Default.Visibility,
                onClick = {
                    showAppVisibilityDialog = true
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
private fun ThemeSelectorItem(focusRequester: FocusRequester? = null) {
    val themeManager = LocalThemeManager.current
    var isFocused by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
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
                            isExpanded = true
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
                            isExpanded = false
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
private fun WallpaperSelectorItem(focusRequester: FocusRequester? = null) {
    val wallpaperManager = LocalWallpaperManager.current
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
                wallpaperManager.setWallpaper(it.toString())
            } catch (_: SecurityException) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fileName = "wallpaper_${System.currentTimeMillis()}.jpg"
                    val outputFile = File(context.filesDir, fileName)
                    inputStream?.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    wallpaperManager.setWallpaper("file://${outputFile.absolutePath}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        isExpanded = false
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
                            isExpanded = true
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
                    isSelected = wallpaperManager.currentWallpaper == null,
                    onClick = {
                        wallpaperManager.clearWallpaper()
                        isExpanded = false
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_image_picker),
                    isSelected = wallpaperManager.currentWallpaper != null &&
                            wallpaperManager.currentWallpaper != WALLPAPER_TRANSPARENT,
                    onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    }
                )

                WallpaperOptionButton(
                    text = stringResource(id = R.string.settings_wallpaper_transparent),
                    isSelected = wallpaperManager.currentWallpaper == WALLPAPER_TRANSPARENT,
                    onClick = {
                        wallpaperManager.setTransparent()
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperOptionButton(
    text: String,
    isSelected: Boolean,
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
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(animatedFocusedScale(isFocused))
                .onFocusChanged {
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
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
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