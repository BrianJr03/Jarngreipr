package jr.brian.jarnfe.themes

import android.content.Context
import androidx.annotation.StringRes
import jr.brian.frontend.look.themes.JarnThemeExporter
import jr.brian.frontend.look.themes.THEME_PREVIEW_FILE
import jr.brian.frontend.look.themes.ThemeExportResult
import jr.brian.frontend.look.themes.ThemeShareVerdict
import jr.brian.frontend.look.themes.themeFileName
import jr.brian.frontend.look.themes.themeShareVerdict
import jr.brian.jarnfe.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hands a theme file to the system share sheet. The host app owns the FileProvider, so it writes
 * nothing itself: Jarn writes into [exportDir] and asks the host to [send] the file from there.
 */
interface JarnThemeShareSheet {
    /** A cache folder the host's FileProvider serves. */
    val exportDir: File

    /** Opens the share sheet for [file]; false when it couldn't. */
    fun send(file: File, title: String): Boolean
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnThemeShareProvider {
    val jarnThemeShareSheet: JarnThemeShareSheet
}

internal sealed interface JarnThemeShareCheck {
    data object Shareable : JarnThemeShareCheck

    /** Not shareable, with the plain reason to show instead of a share button. */
    data class NotShareable(@param:StringRes val messageRes: Int) : JarnThemeShareCheck
}

internal sealed interface JarnThemeShareResult {
    data object Shared : JarnThemeShareResult

    data class NotShared(@param:StringRes val messageRes: Int) : JarnThemeShareResult
}

/**
 * Whether [themeId] can be shared. Only themes saved on the device have a file to share (built-in
 * layouts and templates ship in every Jarn), and an import shares only when its license allows it.
 */
internal fun canShare(themeId: String): JarnThemeShareCheck {
    val entry = JarnThemeRegistry.installed.firstOrNull { it.id == themeId }
    return when {
        entry == null -> JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_missing)
        entry.kind != JarnThemeKind.Imported -> JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_built_in)
        themeShareVerdict(entry.theme.spec.meta) == ThemeShareVerdict.LicenseForbids ->
            JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_license)
        else -> JarnThemeShareCheck.Shareable
    }
}

/**
 * Writes [themeId] as a `.jarntheme` file and opens the share sheet. The file holds the theme and
 * only the files it names, plus its preview when it has one; an import keeps its author and
 * license in the file as credit. Old exports are cleared first so the cache never grows.
 */
internal suspend fun Context.shareTheme(themeId: String): JarnThemeShareResult {
    val check = canShare(themeId)
    if (check is JarnThemeShareCheck.NotShareable) return JarnThemeShareResult.NotShared(check.messageRes)
    val sheet = (applicationContext as? JarnThemeShareProvider)?.jarnThemeShareSheet
        ?: return JarnThemeShareResult.NotShared(R.string.jarnfe_theme_share_failed)
    val entry = JarnThemeRegistry.installed.first { it.id == themeId }
    val spec = entry.theme.spec
    val written = withContext(Dispatchers.IO) {
        sheet.exportDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
        val preview = entry.themeDir?.let { File(it, THEME_PREVIEW_FILE) }
        JarnThemeExporter().write(spec, entry.themeDir, File(sheet.exportDir, themeFileName(spec)), preview)
    }
    if (written !is ThemeExportResult.Written) return JarnThemeShareResult.NotShared(R.string.jarnfe_theme_share_failed)
    val sent = withContext(Dispatchers.Main.immediate) { runCatching { sheet.send(written.file, spec.meta.name) }.getOrDefault(false) }
    return if (sent) JarnThemeShareResult.Shared else JarnThemeShareResult.NotShared(R.string.jarnfe_theme_share_failed)
}
