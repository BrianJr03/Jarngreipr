package jr.brian.home.ui.components.settings

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.focusableSettingCard
import jr.brian.home.model.HomeTarget
import jr.brian.home.model.MainScreen
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalHomeButtonManager
import jr.brian.home.util.ThorDetection

/**
 * Extras-section child of the Home Button Interception toggle. Lets the user
 * pick which display responds to a hardware Home press: TOP, BOTTOM, or BOTH.
 * When BOTH is selected a companion `MainScreen` picker chooses which display
 * hosts `MainActivity` (and takes focus).
 *
 * The whole selector is dimmed and non-interactive when Home interception is
 * disabled or when no external display is connected — both of those cases
 * make the choice meaningless. The BOTH option is dimmed and non-interactive
 * when the frontend feature is off, since BOTH launches `FrontEndActivity`.
 *
 * Only shown on Thor hardware to match [HomeInterceptionSettingItem] — on
 * stock AOSP the interception service never sees `KEYCODE_HOME` so surfacing
 * the selector would only confuse.
 */
@Composable
fun HomeTargetSelectorItem() {
    if (!ThorDetection.isThor()) return

    val homeButtonManager = LocalHomeButtonManager.current
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val interceptionEnabled by homeButtonManager.interceptionEnabled.collectAsStateWithLifecycle()
    val storedTarget by homeButtonManager.homeTarget.collectAsStateWithLifecycle()
    val mainScreen by homeButtonManager.mainScreen.collectAsStateWithLifecycle()
    val esdeState by esdePreferencesManager.state.collectAsStateWithLifecycle()
    val frontendEnabled = esdeState.frontendEnabled

    val hasExternalDisplay = rememberHasExternalDisplayObserved()
    val selectorEnabled = interceptionEnabled && hasExternalDisplay
    val effectiveTarget = storedTarget
        ?: if (frontendEnabled) HomeTarget.BOTH else HomeTarget.BOTTOM

    val subtitle = when {
        !interceptionEnabled -> stringResource(R.string.home_target_disabled_interception_off)
        !hasExternalDisplay -> stringResource(R.string.home_target_disabled_no_external)
        else -> stringResource(R.string.home_target_description)
    }

    HomeTargetCard(
        enabled = selectorEnabled,
        title = stringResource(R.string.home_target_title),
        subtitle = subtitle,
        selectedTarget = effectiveTarget,
        frontendEnabled = frontendEnabled,
        onTargetSelected = { homeButtonManager.setHomeTarget(it) },
        mainScreen = mainScreen,
        onMainScreenSelected = { homeButtonManager.setMainScreen(it) },
    )
}

/**
 * Observes DisplayManager add/remove events so the selector re-enables the
 * moment an external display is plugged in without requiring the settings
 * screen to be reopened. Local to this file — the read-only sibling
 * [jr.brian.home.ui.util.rememberHasExternalDisplay] already lives in
 * `ui/util/` and its callers there don't want the connect/disconnect churn.
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

@Composable
private fun HomeTargetCard(
    enabled: Boolean,
    title: String,
    subtitle: String,
    selectedTarget: HomeTarget,
    frontendEnabled: Boolean,
    onTargetSelected: (HomeTarget) -> Unit,
    mainScreen: MainScreen,
    onMainScreenSelected: (MainScreen) -> Unit,
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
            text = title,
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

        HomeTargetChipRow(
            enabled = enabled,
            selectedTarget = selectedTarget,
            frontendEnabled = frontendEnabled,
            onTargetSelected = onTargetSelected,
        )

        SelectedTargetSubtitle(
            selectedTarget = selectedTarget,
            frontendEnabled = frontendEnabled,
        )

        AnimatedVisibility(visible = enabled && selectedTarget == HomeTarget.BOTH && frontendEnabled) {
            MainScreenPicker(
                selected = mainScreen,
                onSelected = onMainScreenSelected,
            )
        }
    }
}

@Composable
private fun HomeTargetChipRow(
    enabled: Boolean,
    selectedTarget: HomeTarget,
    frontendEnabled: Boolean,
    onTargetSelected: (HomeTarget) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HomeTargetOptions.forEach { option ->
            val chipEnabled = enabled && (option.target != HomeTarget.BOTH || frontendEnabled)
            SegmentedChip(
                label = stringResource(option.labelRes),
                isSelected = option.target == selectedTarget,
                enabled = chipEnabled,
                onClick = { onTargetSelected(option.target) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectedTargetSubtitle(
    selectedTarget: HomeTarget,
    frontendEnabled: Boolean,
) {
    val option = HomeTargetOptions.first { it.target == selectedTarget }
    val subtitleRes = if (option.target == HomeTarget.BOTH && !frontendEnabled) {
        R.string.home_target_option_both_disabled_subtitle
    } else {
        option.subtitleRes
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(subtitleRes),
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 12.sp,
    )
}

@Composable
private fun MainScreenPicker(
    selected: MainScreen,
    onSelected: (MainScreen) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.home_main_screen_title),
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.home_main_screen_description),
        color = Color.Gray,
        fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MainScreenOptions.forEach { option ->
            SegmentedChip(
                label = stringResource(option.labelRes),
                isSelected = option.screen == selected,
                enabled = true,
                onClick = { onSelected(option.screen) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentedChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    var isFocused by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .height(height)
            .alpha(alpha)
            .scale(animatedFocusedScale(isFocused))
            .background(
                color = when {
                    isSelected -> ThemePrimaryColor.copy(alpha = 0.7f)
                    isFocused -> ThemePrimaryColor.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected || isFocused) 1.dp else 0.dp,
                color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .focusable(enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private data class HomeTargetOption(
    val target: HomeTarget,
    val labelRes: Int,
    val subtitleRes: Int,
)

private data class MainScreenOption(
    val screen: MainScreen,
    val labelRes: Int,
)

private val HomeTargetOptions = listOf(
    HomeTargetOption(
        target = HomeTarget.TOP,
        labelRes = R.string.home_target_option_top,
        subtitleRes = R.string.home_target_option_top_subtitle,
    ),
    HomeTargetOption(
        target = HomeTarget.BOTTOM,
        labelRes = R.string.home_target_option_bottom,
        subtitleRes = R.string.home_target_option_bottom_subtitle,
    ),
    HomeTargetOption(
        target = HomeTarget.BOTH,
        labelRes = R.string.home_target_option_both,
        subtitleRes = R.string.home_target_option_both_subtitle,
    ),
)

private val MainScreenOptions = listOf(
    MainScreenOption(
        screen = MainScreen.TOP,
        labelRes = R.string.home_main_screen_option_top,
    ),
    MainScreenOption(
        screen = MainScreen.BOTTOM,
        labelRes = R.string.home_main_screen_option_bottom,
    ),
)
