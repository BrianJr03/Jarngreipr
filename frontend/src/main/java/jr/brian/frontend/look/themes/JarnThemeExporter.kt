package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ViewSpec
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The file extension Jarn writes and opens. */
const val JARN_THEME_EXTENSION = "jarntheme"

/** A theme file's optional preview, at its root. The importer keeps it like any other image. */
const val THEME_PREVIEW_FILE = "preview.png"

private const val MAX_PREVIEW_BYTES = 1024L * 1024

sealed interface ThemeExportResult {
    /** Written to [file] with [assets] files; [skipped] named files were missing or unsafe and left out. */
    data class Written(val file: File, val assets: Int, val skipped: Int) : ThemeExportResult

    data class Failed(val problem: ThemeImportProblem) : ThemeExportResult
}

/**
 * Writes `.jarntheme` files: `theme.json` plus only the files the theme names, read from [themeDir],
 * under the same caps the importer checks, so whatever this writes installs again. Nothing outside
 * [themeDir] is ever read. Blocking file IO; call off the main thread.
 */
class JarnThemeExporter(private val limits: ThemeArchiveLimits = ThemeArchiveLimits()) {

    fun write(spec: JarnThemeSpec, themeDir: File?, out: File, preview: File? = null): ThemeExportResult {
        val partial = File(out.parentFile, ".${out.name}.partial")
        return try {
            val (files, skipped) = assetsFor(spec, themeDir)
            val previewFile = preview?.takeIf { it.isFile && it.length() <= MAX_PREVIEW_BYTES && THEME_PREVIEW_FILE !in files }
            out.parentFile?.mkdirs()
            ZipOutputStream(partial.outputStream().buffered()).use { zip ->
                zip.add(THEME_FILE) { it.write(JarnThemeFormat.encode(spec).toByteArray()) }
                files.forEach { (path, file) -> zip.add(path) { output -> file.inputStream().use { it.copyTo(output) } } }
                previewFile?.let { file -> zip.add(THEME_PREVIEW_FILE) { output -> file.inputStream().use { it.copyTo(output) } } }
            }
            if (partial.length() > limits.maxArchiveBytes) return ThemeExportResult.Failed(ThemeImportProblem.TooLarge)
            if (!partial.renameTo(out)) {
                out.delete()
                if (!partial.renameTo(out)) return ThemeExportResult.Failed(ThemeImportProblem.Unreadable)
            }
            ThemeExportResult.Written(out, files.size, skipped)
        } catch (e: ThemeImportException) {
            ThemeExportResult.Failed(e.problem)
        } catch (e: IOException) {
            ThemeExportResult.Failed(ThemeImportProblem.Unreadable)
        } finally {
            partial.delete()
        }
    }

    /** The named files that exist inside [themeDir], within the caps, and how many names were left out. */
    private fun assetsFor(spec: JarnThemeSpec, themeDir: File?): Pair<Map<String, File>, Int> {
        val paths = themeFilePaths(spec)
        val root = themeDir?.canonicalFile ?: return emptyMap<String, File>() to paths.size
        val found = sortedMapOf<String, File>()
        paths.forEach { path ->
            if (!isSafeThemePath(path) || themeAssetKind(path) == null) return@forEach
            val file = File(root, path).canonicalFile
            val inside = file.path.startsWith(root.path + File.separator)
            if (inside && file.isFile && file.length() <= limits.maxEntryBytes) found[path] = file
        }
        if (found.size + 1 > limits.maxEntries) throw ThemeImportException(ThemeImportProblem.TooManyEntries)
        if (found.values.sumOf { it.length() } > limits.maxTotalBytes) throw ThemeImportException(ThemeImportProblem.TooLarge)
        return found to (paths.size - found.size)
    }

    private fun ZipOutputStream.add(name: String, write: (ZipOutputStream) -> Unit) {
        putNextEntry(ZipEntry(name))
        write(this)
        closeEntry()
    }
}

/** Every theme-relative file [spec] names: pictures, fallbacks, fonts, system art and narrow patches. */
internal fun themeFilePaths(spec: JarnThemeSpec): Set<String> {
    val paths = linkedSetOf<String>()
    fun add(picture: ThemePicture?) {
        if (picture is ThemePicture.File) paths += picture.path
    }
    fun add(layer: ThemeLayer) = when (layer) {
        is ThemeLayer.Picture -> (listOf(layer.source) + layer.fallbacks).forEach(::add)
        is ThemeLayer.Video -> (listOf(layer.fallback) + layer.fallbacks).forEach(::add)
        else -> Unit
    }
    fun add(primary: ThemePrimary) = add(primary.art)
    fun add(view: ViewSpec?) {
        view ?: return
        add(view.primary)
        view.layers.forEach(::add)
        view.narrow?.layers?.forEach(::add)
    }
    add(spec.views.systems)
    add(spec.views.games)
    with(spec.tokens.type) {
        listOf(display, title, heading, body, label, labelStrong, caption, captionStrong).forEach { style ->
            style?.font?.let { paths += it }
        }
    }
    spec.systemArt.values.forEach { art -> listOfNotNull(art.logo, art.background).forEach { paths += it } }
    return paths
}

/** A share-friendly file name for [spec], such as `art-book.jarntheme`. */
fun themeFileName(spec: JarnThemeSpec): String = "${themeIdFor(spec.meta.name)}.$JARN_THEME_EXTENSION"
