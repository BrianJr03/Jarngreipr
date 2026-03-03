package jr.brian.home.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.ui.theme.managers.IconShapeManager
import jr.brian.home.ui.theme.managers.LocalIconShapeManager
import jr.brian.home.ui.theme.managers.LocalOledModeManager
import jr.brian.home.ui.theme.managers.LocalTabAnimationManager
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.ui.theme.managers.OledModeManager
import jr.brian.home.ui.theme.managers.TabAnimationManager
import jr.brian.home.ui.theme.managers.ThemeManager

@Composable
fun LauncherTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val oledModeManager = remember { OledModeManager(context) }
    val iconShapeManager = remember { IconShapeManager(context) }
    val tabAnimationManager = remember { TabAnimationManager(context) }

    CompositionLocalProvider(
        LocalThemeManager provides themeManager,
        LocalOledModeManager provides oledModeManager,
        LocalIconShapeManager provides iconShapeManager,
        LocalTabAnimationManager provides tabAnimationManager
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