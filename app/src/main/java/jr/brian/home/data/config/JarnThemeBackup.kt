package jr.brian.home.data.config

import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.themes.EsdeRestoreOffer
import jr.brian.frontend.look.themes.readEsdeImportRecord
import jr.brian.frontend.look.themes.writeEsdeRestoreOffer
import kotlinx.serialization.Serializable
import java.io.File

/**
 * v78: the active Jarn theme. Only the choice is backed up, never the theme's files. A null [id]
 * is the built-in theme, so restoring can also switch back to it. An id this device doesn't have
 * is kept as written, and Jarn FE shows the built-in theme until that theme is added.
 *
 * v79: [esde] is where an imported ES-DE theme came from, so a restore on a device without it can
 * offer to bring it back from the same ES-DE folder. Null for any other theme and on older blobs.
 */
@Serializable
data class JarnThemeConfig(
    val id: String? = null,
    val esde: JarnThemeEsdeSourceConfig? = null,
)

/** v79: the ES-DE folder and the choices the import used, from the theme's import record. */
@Serializable
data class JarnThemeEsdeSourceConfig(
    val sourceFolder: String,
    val variant: String? = null,
    val colorScheme: String? = null,
    val aspectRatio: String? = null,
)

internal fun JarnLookStore.themeBackup(): JarnThemeConfig = JarnThemeConfig(id = activeThemeId)

/** Null means the backup predates v78, so the current theme stays. Never touches Command Panel match. */
internal fun JarnLookStore.restoreTheme(backup: JarnThemeConfig?) {
    backup ?: return
    setActiveThemeId(backup.id)
}

/** Adds [JarnThemeConfig.esde] when the active theme was imported from ES-DE. */
internal fun JarnThemeConfig.withEsdeSource(themesRoot: File): JarnThemeConfig {
    val folder = id?.takeIf { it.isNotBlank() && '/' !in it && !it.startsWith(".") }?.let { File(themesRoot, it) }
    val record = folder?.let(::readEsdeImportRecord) ?: return this
    return copy(
        esde = JarnThemeEsdeSourceConfig(record.sourceFolder, record.variant, record.colorScheme, record.aspectRatio),
    )
}

/**
 * After a restore: a backed-up ES-DE theme this device doesn't have becomes a re-import offer for the
 * next launch, which only shows once its folder can be read. A backup without [JarnThemeConfig.esde]
 * (older than v79, or another kind of theme) changes nothing; an offer left from before ends by itself
 * once the active theme is a different one.
 */
internal fun restoreEsdeOffer(backup: JarnThemeConfig?, themesRoot: File, offerFile: File) {
    val id = backup?.id ?: return
    val source = backup.esde ?: return
    if (File(themesRoot, id).isDirectory) return
    val offer = EsdeRestoreOffer(id, source.sourceFolder, source.variant, source.colorScheme, source.aspectRatio)
    runCatching { writeEsdeRestoreOffer(offerFile, offer) }
}
