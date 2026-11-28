package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnChoiceCard
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnLookStore
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.JarnImportedThemes
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeOption
import jr.brian.jarnfe.themes.ThemePreviewEffect
import jr.brian.jarnfe.themes.JarnThemeRegistry
import jr.brian.jarnfe.themes.applyThemeChoice
import jr.brian.jarnfe.themes.jarnThemeOptions
import jr.brian.jarnfe.themes.nextOverride
import jr.brian.jarnfe.themes.rememberSavedThemeChoice

private val ThemeCardHeight = 224.dp
private const val THEME_BLURB_LINES = 3

/**
 * Look > Layout: the layout cards, then a row per view that can override the layout. Both screens
 * follow the focused card until it loses focus.
 */
@Composable
internal fun JarnThemeSection(store: JarnLookStore) {
    val saved = rememberSavedThemeChoice()
    val installed = JarnThemeRegistry.installed
    val options = remember(saved, installed) { jarnThemeOptions(saved, installed) }
    var focusedKey by remember { mutableStateOf<String?>(null) }
    var optionsFor by remember { mutableStateOf<JarnThemeEntry?>(null) }
    val focused = options.firstOrNull { it.key == focusedKey }
    LiveThemePreview(focused?.choice)
    JarnThemeCardOptionsInput(focused?.entry) { optionsFor = it }
    JarnThemeCardOptions(optionsFor) { optionsFor = null }
    SettingsGroup(R.string.jarnfe_look_theme_title) {
        JarnChoiceGrid(options) { option, cellModifier ->
            JarnThemeCard(
                option = option,
                onClick = { store.applyThemeChoice(option.choice) },
                modifier = cellModifier.onFocusChanged { state ->
                    focusedKey = nextFocusedLook(focusedKey, option.key, state.isFocused)
                },
            )
        }
        ThemeView.entries.forEach { view ->
            ViewOverrideRow(view, saved.override(view)) {
                store.applyThemeChoice(saved.withOverride(view, saved.override(view).nextOverride()))
            }
        }
        JarnBuildLayoutRow(saved.layout)
        JarnThemeImportRows(store, saved)
    }
}

@Composable
private fun JarnThemeCard(option: JarnThemeOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    JarnChoiceCard(
        name = option.entry.displayName(),
        chosen = option.chosen,
        onClick = onClick,
        modifier = modifier,
        supporting = stringResource(option.entry.blurb),
        supportingLines = THEME_BLURB_LINES,
        height = ThemeCardHeight,
    ) { JarnThemeSketch(option.entry) }
}

@Composable
private fun ViewOverrideRow(view: ThemeView, override: JarnBrowseView?, onClick: () -> Unit) {
    JarnValueRow(
        label = stringResource(view.overrideLabelRes),
        value = stringResource(override.overrideValueRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_theme_override_hint),
    )
}

@Composable
private fun LiveThemePreview(focused: JarnThemeChoice?) {
    ThemePreviewEffect(focused.takeIf { LocalJarnScreenActive.current })
}

@Composable
internal fun JarnThemeEntry.displayName(): String = nameRes?.let { stringResource(it) } ?: theme.spec.meta.name

/** A layout with its own colors always says so, since picking it turns Command Panel matching off. */
@get:StringRes
private val JarnThemeEntry.blurb: Int
    get() = when {
        ownColors -> R.string.jarnfe_theme_own_colors_blurb
        else -> blurbRes ?: R.string.jarnfe_theme_follows_look_blurb
    }

@get:StringRes
private val ThemeView.overrideLabelRes: Int
    get() = when (this) {
        ThemeView.Systems -> R.string.jarnfe_section_systems
        ThemeView.Games -> R.string.jarnfe_theme_games_override
    }

@get:StringRes
private val JarnBrowseView?.overrideValueRes: Int
    get() = when (this) {
        JarnBrowseView.Grid -> R.string.jarnfe_browse_view_grid
        JarnBrowseView.Carousel -> R.string.jarnfe_browse_view_carousel
        JarnBrowseView.SameAsLayout, null -> R.string.jarnfe_theme_same_as_layout
    }
