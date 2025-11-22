package jr.brian.home.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val wallpaperManager = remember { WallpaperManager(context) }

    CompositionLocalProvider(
        LocalThemeManager provides themeManager,
        LocalWallpaperManager provides wallpaperManager
    ) {
        MaterialTheme(
            colorScheme =
                MaterialTheme.colorScheme.copy(
                    primary = AppRed,
                    secondary = AppBlue,
                    background = AppDarkBlue,
                ),
            content = content,
        )
    }
}