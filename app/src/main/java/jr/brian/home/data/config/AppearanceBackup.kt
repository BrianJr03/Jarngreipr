package jr.brian.home.data.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.managers.AppearanceRestoreSignal
import jr.brian.home.ui.theme.managers.IconShape
import jr.brian.home.ui.theme.managers.IconShapeManager
import jr.brian.home.ui.theme.managers.OledModeManager
import jr.brian.home.ui.theme.managers.StoredCustomTheme
import jr.brian.home.ui.theme.managers.StoredThemes
import jr.brian.home.ui.theme.managers.TabAnimationManager
import jr.brian.home.ui.theme.managers.isStorableThemeText
import jr.brian.home.ui.theme.managers.readStoredThemes
import jr.brian.home.ui.theme.managers.writeStoredThemes
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_CUSTOM_THEMES = 100
private const val MAX_THEME_NAME_LENGTH = 64
private val ThemeHex = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

/** Each field is nullable so a partial backup only changes what it carries. */
@Serializable
data class AppearanceConfig(
    val themeId: String? = null,
    val customThemes: List<CustomThemeConfig>? = null,
    val oledMode: Boolean? = null,
    val keyboardOledExempt: Boolean? = null,
    val iconShape: String? = null,
    val tabAnimation: Boolean? = null,
)

@Serializable
data class CustomThemeConfig(
    val id: String = "",
    val name: String = "",
    val primaryHex: String = "",
    val secondaryHex: String = "",
    val isSolid: Boolean = false,
)

internal fun StoredCustomTheme.toConfig() = CustomThemeConfig(id, name, primaryHex, secondaryHex, isSolid)

private fun CustomThemeConfig.toStored() = StoredCustomTheme(id, name, primaryHex, secondaryHex, isSolid)

internal fun CustomThemeConfig.toStoredOrNull(): StoredCustomTheme? {
    val valid = id.startsWith(ColorTheme.CUSTOM_THEME_PREFIX) &&
        name.length <= MAX_THEME_NAME_LENGTH &&
        listOf(id, name).all(::isStorableThemeText) &&
        ThemeHex.matches(primaryHex) &&
        ThemeHex.matches(secondaryHex)
    return if (valid) toStored() else null
}

/**
 * Backed-up themes replace ones with the same id and are added after the rest. Current themes are
 * never dropped, and only checked backup entries get in.
 */
internal fun mergeCustomThemes(
    current: List<CustomThemeConfig>,
    backup: List<CustomThemeConfig>?,
): List<CustomThemeConfig> {
    if (backup == null) return current
    val incoming = backup.filter { it.toStoredOrNull() != null }
        .distinctBy { it.id }
        .associateBy { it.id }
    val kept = current.map { incoming[it.id] ?: it }
    val keptIds = kept.map { it.id }.toSet()
    val added = incoming.values.filter { it.id !in keptIds }
    return kept + added.take((MAX_CUSTOM_THEMES - kept.size).coerceAtLeast(0))
}

/** A preset id, or a custom id that exists after the merge. Anything else keeps [current]. */
internal fun resolveThemeId(backup: String?, current: String?, customs: List<CustomThemeConfig>): String? {
    if (backup == null) return current
    val known = ColorTheme.presetThemes.any { it.id == backup } || customs.any { it.id == backup }
    return if (known) backup else current
}

internal fun resolveIconShape(backup: String?, current: String?): String? =
    backup?.takeIf { name -> IconShape.entries.any { it.name == name } } ?: current

/** What [current] becomes after restoring [backup]. */
internal fun AppearanceConfig.mergedInto(current: AppearanceConfig): AppearanceConfig {
    val customs = mergeCustomThemes(current.customThemes.orEmpty(), customThemes)
    return AppearanceConfig(
        themeId = resolveThemeId(themeId, current.themeId, customs),
        customThemes = customs,
        oledMode = oledMode ?: current.oledMode,
        keyboardOledExempt = keyboardOledExempt ?: current.keyboardOledExempt,
        iconShape = resolveIconShape(iconShape, current.iconShape),
        tabAnimation = tabAnimation ?: current.tabAnimation,
    )
}

/** Reads and writes the look settings Classic keeps outside the injected managers. */
@Singleton
class AppearanceBackup @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun export(): AppearanceConfig {
        val themes = context.readStoredThemes()
        val oled = OledModeManager(context)
        return AppearanceConfig(
            themeId = themes.selectedId ?: ColorTheme.PINK_VIOLET.id,
            customThemes = themes.custom.map { it.toConfig() },
            oledMode = oled.isOledModeEnabled,
            keyboardOledExempt = oled.isKeyboardOledExempt,
            iconShape = IconShapeManager(context).iconShape.name,
            tabAnimation = TabAnimationManager(context).isTabAnimationEnabled,
        )
    }

    fun restore(backup: AppearanceConfig?) {
        if (backup == null) return
        val current = export()
        val merged = backup.mergedInto(current)
        context.writeStoredThemes(
            StoredThemes(
                selectedId = merged.themeId,
                custom = merged.customThemes.orEmpty().map { it.toStored() },
            )
        )
        val oled = OledModeManager(context)
        merged.oledMode?.let(oled::setOledMode)
        merged.keyboardOledExempt?.let(oled::updateKeyboardOledExempt)
        merged.iconShape?.let { IconShapeManager(context).updateIconShape(IconShape.valueOf(it)) }
        merged.tabAnimation?.let(TabAnimationManager(context)::setTabAnimation)
        AppearanceRestoreSignal.notifyRestored()
    }
}
