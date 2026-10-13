package jr.brian.home.ui.components.settings

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.focusableSettingCard
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalHomeButtonManager
import jr.brian.home.util.ThorDetection

/**
 * Extras-section info card that documents the three Home-press gestures
 * handled by [jr.brian.home.service.HomeInterceptorService]:
 *
 *  - press once  → launcher on bottom
 *  - double press → launcher on top
 *  - long press  → launcher on bottom + frontend on top
 *
 * Replaces the earlier BOTTOM/BOTH chip selector — the three gestures are
 * always active while Home Button Interception is on, so the UI's job here is
 * to describe them, not let the user pick between them. The card is dimmed
 * when the gestures are inert (interception disabled, no external display) so
 * the surface mirrors the real routing state.
 *
 * Only shown on Thor hardware to match [HomeInterceptionSettingItem] — on
 * stock AOSP the interception service never sees `KEYCODE_HOME` so surfacing
 * the card there would only confuse.
 */
@Composable
fun HomeGesturesInfoCard() {
    if (!ThorDetection.isThor()) return

    val homeButtonManager = LocalHomeButtonManager.current
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val interceptionEnabled by homeButtonManager.interceptionEnabled.collectAsStateWithLifecycle()
    val esdeState by esdePreferencesManager.state.collectAsStateWithLifecycle()
    val frontendEnabled = esdeState.frontendEnabled

    val hasExternalDisplay = rememberHasExternalDisplayObserved()
    val enabled = interceptionEnabled && hasExternalDisplay

    val subtitle = when {
        !interceptionEnabled ->
            stringResource(R.string.home_gestures_card_disabled_interception_off)
        !hasExternalDisplay ->
            stringResource(R.string.home_gestures_card_disabled_no_external)
        else -> stringResource(R.string.home_gestures_card_description)
    }

    HomeGesturesCardBody(
        enabled = enabled,
        subtitle = subtitle,
        showFrontendNote = enabled && !frontendEnabled,
    )
}

@Composable
private fun HomeGesturesCardBody(
    enabled: Boolean,
    subtitle: String,
    showFrontendNote: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .focusableSettingCard(isFocused)
            .focusable(enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_gestures_card_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GestureRow(
                gesture = stringResource(R.string.home_gestures_single_press_label),
                detail = stringResource(R.string.home_gestures_single_press_detail),
            )
            GestureRow(
                gesture = stringResource(R.string.home_gestures_double_press_label),
                detail = stringResource(R.string.home_gestures_double_press_detail),
            )
            GestureRow(
                gesture = stringResource(R.string.home_gestures_long_press_label),
                detail = stringResource(R.string.home_gestures_long_press_detail),
            )
        }

        if (showFrontendNote) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_gestures_card_frontend_disabled_note),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun GestureRow(gesture: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = gesture,
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 110.dp),
        )
        Text(
            text = detail,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
        )
    }
}

/**
 * Observes DisplayManager add/remove events so the card re-enables the moment
 * an external display is plugged in without requiring the settings screen to
 * be reopened.
 */
@Composable
private fun rememberHasExternalDisplayObserved(): Boolean {
    val context = LocalContext.current
    var hasExternal by remember { mutableStateOf(currentHasExternalDisplay(context)) }
    DisposableEffect(context) {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val recompute = { hasExternal = displayManager.displays.size > 1 }
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = recompute()
            override fun onDisplayRemoved(displayId: Int) = recompute()
            override fun onDisplayChanged(displayId: Int) = recompute()
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }
    return hasExternal
}

private fun currentHasExternalDisplay(context: Context): Boolean {
    val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return displayManager.displays.size > 1
}
