package jr.brian.home.esde.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.esde.preferences.OverlayMediaType
import jr.brian.home.esde.ui.components.LogoAlignmentSelector
import jr.brian.home.esde.ui.components.MarqueeSizeSetting
import jr.brian.home.esde.ui.components.MarqueeTabSettingsOption
import jr.brian.home.esde.ui.components.OverlayMediaTypeSelector
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.model.PageType
import jr.brian.home.model.Shortcut

@Composable
fun MarqueeSectionContent(
    prefsState: ESDEPrefsState,
    pageTypes: List<PageType>,
    onLogoAlignmentChange: (LogoAlignment) -> Unit,
    onMarqueeWidthChange: (Int) -> Unit,
    onMarqueeHeightChange: (Int) -> Unit,
    onMarqueeSizeReset: () -> Unit,
    onNavigateToMarqueePressShortcut: () -> Unit,
    onToggleMarqueePageVisibility: (Int) -> Unit,
    onToggleDescriptionOverlayPage: (Int) -> Unit,
    onShowMarqueeForSystemChange: (Boolean) -> Unit,
    onShowMarqueeForGameChange: (Boolean) -> Unit,
    onMarqueeMinWidthPercentChange: (Float) -> Unit,
    onOverlayMediaTypeChange: (OverlayMediaType) -> Unit
) {
    val pageCount = pageTypes.size

    LogoAlignmentSelector(
        selectedAlignment = prefsState.logoAlignment,
        onAlignmentSelected = { alignment ->
            onLogoAlignmentChange(alignment)
        }
    )

    OverlayMediaTypeSelector(
        selectedType = prefsState.overlayMediaType,
        onTypeSelected = { type ->
            onOverlayMediaTypeChange(type)
        }
    )

    MarqueeSizeSetting(
        title = stringResource(R.string.esde_settings_logo_size),
        description = stringResource(R.string.esde_settings_logo_size_description),
        width = prefsState.marqueeWidth,
        height = prefsState.marqueeHeight,
        widthLabel = stringResource(R.string.esde_settings_logo_width),
        heightLabel = stringResource(R.string.esde_settings_logo_height),
        resetLabel = stringResource(R.string.esde_settings_logo_size_reset),
        onWidthChange = onMarqueeWidthChange,
        onHeightChange = onMarqueeHeightChange,
        onReset = onMarqueeSizeReset
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_marquee_min_width),
        value = prefsState.marqueeMinWidthPercent,
        valueRange = 0.3f..1.0f,
        steps = 6,
        valueText = "${(prefsState.marqueeMinWidthPercent * 100).toInt()}%",
        onValueChange = { percent ->
            onMarqueeMinWidthPercentChange(percent)
        },
        description = stringResource(R.string.esde_settings_marquee_min_width_description)
    )

    val currentShortcutLabel = when (prefsState.marqueePressShortcut) {
        Shortcut.NONE -> stringResource(R.string.shortcut_none)
        Shortcut.SETTINGS -> stringResource(R.string.shortcut_settings)
        Shortcut.APP_SEARCH -> stringResource(R.string.shortcut_app_search)
        Shortcut.POWERED_OFF -> stringResource(R.string.shortcut_powered_off)
        Shortcut.QUICK_DELETE -> stringResource(R.string.shortcut_quick_delete)
        Shortcut.CUSTOM_THEME -> stringResource(R.string.shortcut_custom_theme)
        Shortcut.MONITOR -> stringResource(R.string.shortcut_monitor)
        Shortcut.CONTROL_PAD -> stringResource(R.string.shortcut_control_pad)
        Shortcut.VOLUME_CONTROLS -> stringResource(R.string.shortcut_volume_controls)
        Shortcut.RECENT_APPS -> stringResource(R.string.shortcut_recent_apps)
        Shortcut.APP -> stringResource(R.string.shortcut_app)
    }

    ToggleSetting(
        title = stringResource(R.string.marquee_press_shortcut_screen_title),
        description = stringResource(
            R.string.marquee_press_shortcut_screen_description
        ) + "\n" + stringResource(
            R.string.esde_settings_marquee_press_shortcut_choose
        ) + ": $currentShortcutLabel",
        checked = false,
        showToggle = false,
        onClick = onNavigateToMarqueePressShortcut
    )

    ToggleSetting(
        title = stringResource(R.string.esde_settings_marquee_show_for_system),
        description = stringResource(R.string.esde_settings_marquee_show_for_system_description),
        checked = prefsState.showMarqueeForSystem,
        onCheckedChange = { show -> onShowMarqueeForSystemChange(show) }
    )

    ToggleSetting(
        title = stringResource(R.string.esde_settings_marquee_show_for_game),
        description = stringResource(R.string.esde_settings_marquee_show_for_game_description),
        checked = prefsState.showMarqueeForGame,
        onCheckedChange = { show -> onShowMarqueeForGameChange(show) }
    )

    if (pageCount > 1) {
        Column {
            Text(
                text = stringResource(R.string.esde_settings_marquee_tab_settings_title),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.esde_settings_marquee_tab_settings_description),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (pageIndex in 0 until pageCount) {
                    val isPageVisible =
                        !prefsState.marqueeHiddenPages.contains(pageIndex)
                    val isDescriptionOverlayEnabled =
                        prefsState.descriptionOverlayEnabledPages.contains(pageIndex)

                    MarqueeTabSettingsOption(
                        pageIndex = pageIndex,
                        isVisible = isPageVisible,
                        isDescriptionOverlayEnabled = isDescriptionOverlayEnabled,
                        onVisibilityToggle = {
                            onToggleMarqueePageVisibility(pageIndex)
                        },
                        onDescriptionOverlayToggle = {
                            onToggleDescriptionOverlayPage(pageIndex)
                        }
                    )
                }
            }
        }
    }
}
