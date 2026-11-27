package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnChoiceCard
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.JarnThemeOption
import jr.brian.jarnfe.themes.JarnThemePreview
import jr.brian.jarnfe.themes.JarnThemeRegistry
import jr.brian.jarnfe.themes.activeTheme
import jr.brian.jarnfe.themes.applyThemeChoice
import jr.brian.jarnfe.themes.jarnThemeOptions
import jr.brian.jarnfe.themes.rememberSavedThemeChoice

private val ThemeCardHeight = 196.dp

/** Look > Theme: the layout cards. Both screens follow the focused card until it loses focus. */
@Composable
internal fun JarnThemeSection(store: JarnLookStore) {
    val saved = rememberSavedThemeChoice()
    val display by store.displayState.collectAsState()
    val options = remember(saved, display.systemView, display.gameView) { jarnThemeOptions(saved, display) }
    var focusedKey by remember { mutableStateOf<String?>(null) }
    LiveThemePreview(options.firstOrNull { it.key == focusedKey }?.choice)
    SettingsGroup(R.string.jarnfe_look_theme_title) {
        JarnChoiceGrid(options) { option, cellModifier ->
            JarnThemeCard(
                option = option,
                display = display,
                onClick = { store.applyThemeChoice(option.choice) },
                modifier = cellModifier.onFocusChanged { state ->
                    focusedKey = nextFocusedLook(focusedKey, option.key, state.isFocused)
                },
            )
        }
    }
}

@Composable
private fun JarnThemeCard(
    option: JarnThemeOption,
    display: JarnDisplayPrefs,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = remember(option.choice, display.systemView, display.gameView) { option.choice.activeTheme(display) }
    JarnChoiceCard(
        name = option.choice.displayName(),
        chosen = option.chosen,
        onClick = onClick,
        modifier = modifier,
        supporting = stringResource(option.choice.blurbRes),
        height = ThemeCardHeight,
    ) { JarnThemeSketch(theme, Modifier.weight(1f)) }
}

@Composable
private fun LiveThemePreview(focused: JarnThemeChoice?) {
    val preview = focused.takeIf { LocalJarnScreenActive.current }
    DisposableEffect(preview) {
        JarnThemePreview.choice = preview
        onDispose { JarnThemePreview.choice = null }
    }
}

@Composable
private fun JarnThemeChoice.displayName(): String = when (this) {
    is JarnThemeChoice.BuiltIn -> if (mixed) {
        stringResource(R.string.jarnfe_theme_mixed)
    } else {
        JarnThemeRegistry.builtInFor(systems).nameRes?.let { stringResource(it) }.orEmpty()
    }
    is JarnThemeChoice.Installed -> entry.nameRes?.let { stringResource(it) } ?: entry.theme.spec.meta.name
}

/** A theme with its own colors always says so, since picking it turns Command Panel matching off. */
@get:StringRes
private val JarnThemeChoice.blurbRes: Int
    get() = when (this) {
        is JarnThemeChoice.BuiltIn -> when {
            !mixed -> JarnThemeRegistry.builtInFor(systems).blurbRes ?: R.string.jarnfe_theme_follows_look_blurb
            systems == JarnBrowseView.Grid -> R.string.jarnfe_theme_mixed_grid_systems
            else -> R.string.jarnfe_theme_mixed_carousel_systems
        }
        is JarnThemeChoice.Installed -> when {
            entry.ownColors -> R.string.jarnfe_theme_own_colors_blurb
            else -> entry.blurbRes ?: R.string.jarnfe_theme_follows_look_blurb
        }
    }
