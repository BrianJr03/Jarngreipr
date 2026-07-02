package jr.brian.home.ui.components.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.FloatyModeManager
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.util.SettingsScreenUtil.DEFAULT_VERSION_NAME

private const val TAPS_TO_UNLOCK = 8

@Composable
fun ScreenHeader(
    showVersion: Boolean = false,
    onVersionTapCountdown: (Int) -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val floatyModeManager = LocalFloatyModeManager.current
    val showBackButton = appVisibilityManager.showSettingsBackButton
    if (!showBackButton && !showVersion) return

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ?: DEFAULT_VERSION_NAME
    } catch (_: Exception) {
        DEFAULT_VERSION_NAME
    }

    var tapCount by remember { mutableIntStateOf(0) }

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
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        tapCount = handleFloatyUnlockTap(
                            context = context,
                            floatyModeManager = floatyModeManager,
                            tapCount = tapCount,
                            onVersionTapCountdown = onVersionTapCountdown
                        )
                    }
            )
        }
    }
}
private fun handleFloatyUnlockTap(
    context: Context,
    floatyModeManager: FloatyModeManager,
    tapCount: Int,
    onVersionTapCountdown: (Int) -> Unit
): Int {
    if (floatyModeManager.isUnlocked) {
        Toast.makeText(
            context,
            context.getString(R.string.floaty_mode_already_unlocked),
            Toast.LENGTH_SHORT
        ).show()
        return tapCount
    }
    val updatedTapCount = tapCount + 1
    val countdownValue = (TAPS_TO_UNLOCK - updatedTapCount).coerceAtLeast(0)
    onVersionTapCountdown(countdownValue)
    if (updatedTapCount < TAPS_TO_UNLOCK) return updatedTapCount
    floatyModeManager.unlock()
    Toast.makeText(
        context,
        context.getString(R.string.floaty_mode_unlocked),
        Toast.LENGTH_LONG
    ).show()
    return 0
}
