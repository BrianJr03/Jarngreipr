package jr.brian.home.ui.components.dialog

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp

/**
 * Bridges tap-to-launch with the per-app "ask on launch" flag. Call [launch]
 * instead of [launchApp] at every tile-tap site; place [DialogIfNeeded] once
 * in the composable subtree so the chooser can render.
 *
 * When the flag is off (default) or the device has no external display, this
 * is a straight passthrough to [launchApp]. Choices are one-shot: the app's
 * stored [DisplayPreference] is unchanged so the next tap prompts again.
 */
class DisplayChooserController internal constructor(
    private val prefs: AppDisplayPreferenceManager,
    private val hasExternalDisplay: Boolean
) {
    private var pending by mutableStateOf<PendingLaunch?>(null)

    fun launch(
        context: Context,
        packageName: String,
        currentPreference: DisplayPreference,
        intent: Intent? = null
    ) {
        val shouldPrompt = hasExternalDisplay &&
            prefs.getPromptForDisplayOnLaunch(packageName)
        if (!shouldPrompt) {
            launchApp(context, packageName, currentPreference, intent)
            return
        }
        pending = PendingLaunch(context, packageName, intent)
    }

    @Composable
    fun DialogIfNeeded() {
        val request = pending ?: return
        DisplayChooserDialog(
            onPick = { preference ->
                launchApp(request.context, request.packageName, preference, request.intent)
                pending = null
            },
            onDismiss = { pending = null }
        )
    }

    private data class PendingLaunch(
        val context: Context,
        val packageName: String,
        val intent: Intent?
    )
}

@Composable
fun rememberDisplayChooser(): DisplayChooserController {
    val prefs = LocalAppDisplayPreferenceManager.current
    val hasExternal = rememberHasExternalDisplay()
    return remember(prefs, hasExternal) {
        DisplayChooserController(prefs, hasExternal)
    }
}

@Composable
private fun DisplayChooserDialog(
    onPick: (DisplayPreference) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.display_chooser_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DisplayChoiceCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.app_options_launch_primary_descr),
                    icon = Icons.Default.Tv,
                    onClick = { onPick(DisplayPreference.PRIMARY_DISPLAY) }
                )
                DisplayChoiceCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.app_options_launch_external_descr),
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { onPick(DisplayPreference.CURRENT_DISPLAY) }
                )
            }
        }
    }
}

@Composable
private fun DisplayChoiceCard(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .focusable()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
