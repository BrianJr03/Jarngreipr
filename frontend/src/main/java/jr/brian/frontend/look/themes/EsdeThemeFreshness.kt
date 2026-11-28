package jr.brian.frontend.look.themes

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val THEMES_DIR = "themes"

sealed interface EsdeThemeFreshnessResult {
    data object Unchanged : EsdeThemeFreshnessResult

    /** The ES-DE folder changed since the import. [current] is its stamp now. */
    data class Updated(val source: File, val current: EsdeThemeStamp) : EsdeThemeFreshnessResult

    /**
     * The ES-DE folder can't be seen: no access, an SD card out, or the folder moved. Never a
     * reason to delete anything; the last import simply stays.
     */
    data object SourceMissing : EsdeThemeFreshnessResult
}

object EsdeThemeFreshness {
    /**
     * Compares [record] with its ES-DE folder now. Only the exact folder imported counts: a same-named
     * copy elsewhere is a different theme, and an SD card keeps its path while mounted. A folder that
     * is gone or no longer a theme (no `capabilities.xml`) is missing, never updated.
     */
    fun check(
        record: EsdeImportRecord,
        locator: EsdeThemeLocator,
        readStamp: (File) -> EsdeThemeStamp? = ::readEsdeThemeStamp,
    ): EsdeThemeFreshnessResult {
        if (locator.scan() == EsdeThemeScan.NoAccess) return EsdeThemeFreshnessResult.SourceMissing
        val source = File(record.sourceFolder)
        val current = readStamp(source)?.takeIf { it.capabilitiesModifiedMs != null }
            ?: return EsdeThemeFreshnessResult.SourceMissing
        return when (current) {
            record.stamp, record.dismissedStamp -> EsdeThemeFreshnessResult.Unchanged
            else -> EsdeThemeFreshnessResult.Updated(source, current)
        }
    }
}

/** An installed theme whose ES-DE source changed, for the "Re-import?" prompt. */
data class EsdeThemeUpdate(val themeId: String, val record: EsdeImportRecord, val source: File, val current: EsdeThemeStamp)

/**
 * The launch check behind "Your ES-DE theme updated. Re-import?". Only reads ES-DE's folders;
 * the only file it writes is the installed theme's own record, when the player says no.
 */
@Singleton
class EsdeThemeUpdates internal constructor(
    private val themesRoot: File,
    private val locator: EsdeThemeLocator,
    private val readStamp: (File) -> EsdeThemeStamp?,
) {
    @Inject
    constructor(@ApplicationContext context: Context, locator: EsdeThemeLocator) :
        this(File(context.filesDir, THEMES_DIR), locator, ::readEsdeThemeStamp)

    /** The update to offer for [themeId], usually the active theme, or null when there is none. */
    suspend fun updateFor(themeId: String): EsdeThemeUpdate? = withContext(Dispatchers.IO) {
        val folder = themeFolder(themeId) ?: return@withContext null
        val record = readEsdeImportRecord(folder) ?: return@withContext null
        when (val result = EsdeThemeFreshness.check(record, locator, readStamp)) {
            is EsdeThemeFreshnessResult.Updated -> EsdeThemeUpdate(themeId, record, result.source, result.current)
            EsdeThemeFreshnessResult.Unchanged, EsdeThemeFreshnessResult.SourceMissing -> null
        }
    }

    /** "Not now": this change isn't offered again, a later one is. */
    suspend fun dismiss(update: EsdeThemeUpdate) = withContext(Dispatchers.IO) {
        val folder = themeFolder(update.themeId) ?: return@withContext
        val record = readEsdeImportRecord(folder) ?: return@withContext
        runCatching { writeEsdeImportRecord(folder, record.copy(dismissedStamp = update.current)) }
    }

    private fun themeFolder(themeId: String): File? =
        themeId.takeIf { isSafeThemePath(it) && '/' !in it }?.let { File(themesRoot, it) }?.takeIf(File::isDirectory)
}
