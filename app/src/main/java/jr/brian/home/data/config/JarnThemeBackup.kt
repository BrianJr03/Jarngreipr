package jr.brian.home.data.config

import jr.brian.frontend.look.JarnLookStore
import kotlinx.serialization.Serializable

/**
 * v78: the active Jarn theme. Only the choice is backed up, never the theme's files. A null [id]
 * is the built-in theme, so restoring can also switch back to it. An id this device doesn't have
 * is kept as written, and Jarn FE shows the built-in theme until that theme is added.
 */
@Serializable
data class JarnThemeConfig(
    val id: String? = null,
)

internal fun JarnLookStore.themeBackup(): JarnThemeConfig = JarnThemeConfig(id = activeThemeId)

/** Null means the backup predates v78, so the current theme stays. Never touches Command Panel match. */
internal fun JarnLookStore.restoreTheme(backup: JarnThemeConfig?) {
    backup ?: return
    setActiveThemeId(backup.id)
}
