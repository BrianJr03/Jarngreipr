package jr.brian.jarnfe.themes

import android.content.Context
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import java.io.File

/** The folder the theme installer uses too, so made and imported themes sit side by side. */
private const val THEMES_DIR = "themes"
private const val THEME_FILE = "theme.json"
private const val DRAFT_FILE = "draft.json"
private const val HISTORY_DIR = "history"
private const val HISTORY_LIMIT = 20
private const val MAX_ID_LENGTH = 48
private const val FALLBACK_ID = "my-layout"
private val NonIdChars = Regex("[^a-z0-9]+")

/** A theme on this device, made here or imported. */
internal data class StoredTheme(val id: String, val folder: File, val spec: JarnThemeSpec)

/** One saved version of a theme, newest first in [JarnUserThemeStore.history]. */
internal data class ThemeVersion(val savedAt: Long, val spec: JarnThemeSpec)

/**
 * Themes in `filesDir/themes/<id>/`: `theme.json` is the saved theme, `draft.json` the editor's
 * autosave, and `history/` the last [HISTORY_LIMIT] saves. Plain files, never backed up; only the
 * active theme's id is. Callers keep this off the main thread.
 */
internal class JarnUserThemeStore(private val root: File, private val now: () -> Long = System::currentTimeMillis) {
    fun list(): List<StoredTheme> = root.listFiles().orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") }
        .sortedBy { it.name }
        .mapNotNull { folder -> read(File(folder, THEME_FILE))?.let { StoredTheme(folder.name, folder, it) } }

    fun theme(id: String): JarnThemeSpec? = read(File(folder(id), THEME_FILE))

    fun draft(id: String): JarnThemeSpec? = read(File(folder(id), DRAFT_FILE))

    fun saveDraft(id: String, spec: JarnThemeSpec) = write(File(folder(id), DRAFT_FILE), spec)

    /** Saves [spec] as the theme and as a new version, and drops the draft it came from. */
    fun save(id: String, spec: JarnThemeSpec) {
        write(File(folder(id), THEME_FILE), spec)
        val history = File(folder(id), HISTORY_DIR).apply { mkdirs() }
        write(File(history, "${now()}.json"), spec)
        history.listFiles().orEmpty().sortedByDescending { it.savedAt }.drop(HISTORY_LIMIT).forEach { it.delete() }
        File(folder(id), DRAFT_FILE).delete()
    }

    fun history(id: String): List<ThemeVersion> = File(folder(id), HISTORY_DIR).listFiles().orEmpty()
        .sortedByDescending { it.savedAt }
        .mapNotNull { file -> read(file)?.let { ThemeVersion(file.savedAt, it) } }

    fun delete(id: String) {
        folder(id).deleteRecursively()
    }

    /** A folder-safe id from [name] that no theme on this device uses yet. */
    fun newId(name: String): String {
        val base = name.lowercase().replace(NonIdChars, "-").trim('-').take(MAX_ID_LENGTH).trim('-').ifEmpty { FALLBACK_ID }
        return (sequenceOf(base) + generateSequence(2) { it + 1 }.map { "$base-$it" }).first { !folder(it).exists() }
    }

    fun folder(id: String): File = File(root, id)

    private fun read(file: File): JarnThemeSpec? {
        if (!file.isFile) return null
        return (runCatching { JarnThemeFormat.decode(file.readText()) }.getOrNull() as? JarnThemeDecodeResult.Decoded)?.spec
    }

    /** Written beside the target and renamed into place, so a crash never leaves half a file. */
    private fun write(file: File, spec: JarnThemeSpec) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, ".${file.name}.tmp")
        temp.writeText(JarnThemeFormat.encode(spec))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    private val File.savedAt: Long get() = nameWithoutExtension.toLongOrNull() ?: 0L
}

internal fun Context.jarnUserThemeStore(): JarnUserThemeStore = JarnUserThemeStore(File(filesDir, THEMES_DIR))
