package jr.brian.home.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.theme.AppBackgroundDark
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.ui.theme.WALLPAPER_TRANSPARENT

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_KEYBOARD_VISIBLE = "keyboard_visible"

@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    isLoading: Boolean = false,
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var keyboardVisible by remember {
        mutableStateOf(prefs.getBoolean(KEY_KEYBOARD_VISIBLE, true))
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedKeyboardIndex by remember { mutableIntStateOf(0) }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(keyboardVisible) {
        prefs.edit {
            putBoolean(KEY_KEYBOARD_VISIBLE, keyboardVisible)
        }
    }

    BackHandler {
        keyboardVisible = !keyboardVisible
    }

    if (showAppInfoDialog && selectedApp != null) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.app_info_title),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.app_info_message, selectedApp!!.label),
                    color = Color.White.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StyledDialogButton(
                        text = stringResource(id = R.string.dialog_cancel),
                        onClick = { showAppInfoDialog = false },
                    )
                    StyledDialogButton(
                        text = stringResource(id = R.string.dialog_app_info),
                        onClick = {
                            selectedApp?.let { app ->
                                openAppInfo(context, app.packageName)
                            }
                            showAppInfoDialog = false
                        },
                        isPrimary = true,
                    )
                }
            },
            dismissButton = {},
            containerColor = AppCardDark,
            shape = RoundedCornerShape(16.dp),
        )
    }

    val wallpaperManager = LocalWallpaperManager.current
    val wallpaperUri = wallpaperManager.currentWallpaper

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
    ) {
        when {
            wallpaperUri == WALLPAPER_TRANSPARENT -> {
                // Transparent - system wallpaper shows through
                // No background needed
            }

            wallpaperUri != null -> {
                // Custom image wallpaper
                Image(
                    painter = rememberAsyncImagePainter(wallpaperUri.toUri()),
                    contentDescription = "Wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                // Default background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackgroundDark)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppSelectionContent(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                columns = 4,
                keyboardFocusRequesters = keyboardFocusRequesters,
                appFocusRequesters = appFocusRequesters,
                savedKeyboardIndex = savedKeyboardIndex,
                savedAppIndex = savedAppIndex,
                onKeyboardFocusChanged = { savedKeyboardIndex = it },
                onAppFocusChanged = { savedAppIndex = it },
                onAppClick = { app ->
                    launchApp(
                        context = context,
                        packageName = app.packageName
                    )
                },
                onAppLongClick = { app ->
                    selectedApp = app
                    showAppInfoDialog = true
                },
                keyboardVisible = keyboardVisible,
                onSettingsClick = onSettingsClick,
            )
        }
    }
}

private fun launchApp(
    context: Context,
    packageName: String
) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openAppInfo(
    context: Context,
    packageName: String
) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}