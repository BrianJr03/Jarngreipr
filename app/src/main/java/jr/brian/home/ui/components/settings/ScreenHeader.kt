package jr.brian.home.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.util.SettingsScreenUtil.DEFAULT_VERSION_NAME

@Composable
fun ScreenHeader(
    showVersion: Boolean = false,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val showBackButton = appVisibilityManager.showSettingsBackButton
    if (!showBackButton && !showVersion) return

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ?: DEFAULT_VERSION_NAME
    } catch (_: Exception) {
        DEFAULT_VERSION_NAME
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        if (showBackButton) {
            BackButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        if (showVersion) {
            Text(
                text = stringResource(R.string.settings_version_label, versionName),
                color = ThemeAccentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
