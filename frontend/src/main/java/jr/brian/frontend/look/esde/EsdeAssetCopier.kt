package jr.brian.frontend.look.esde

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import jr.brian.frontend.look.themes.isImageWithinCaps
import jr.brian.frontend.look.themes.isUsableFont
import jr.brian.frontend.look.themes.readImageSize
import java.io.File
import java.io.IOException

/** What the copy may spend in the theme folder, like a `.jarntheme` file's own cap. */
const val ESDE_ASSET_CAP_BYTES = 64L * 1024 * 1024
private const val MAX_FONT_BYTES = 8L * 1024 * 1024
private const val HEADER_BYTES = 64 * 1024
private const val JPEG_QUALITY = 88
private const val ASSET_DIR = "esde"
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
private val FONT_EXTENSIONS = setOf("ttf", "otf")

/** The largest a copied picture needs to be: [background] for full-screen art, [logo] for logos. */
data class EsdeAssetSizes(val background: Size = Size(1920, 1080), val logo: Size = Size(800, 450)) {
    data class Size(val width: Int, val height: Int)
}

/**
 * Shrinks [source] to fit [maxWidth] by [maxHeight] and writes it to [target], PNG when
 * [keepAlpha], else JPEG. An animated GIF keeps its first frame. False when it can't be decoded.
 */
fun interface EsdeImageScaler {
    fun scale(source: File, target: File, maxWidth: Int, maxHeight: Int, keepAlpha: Boolean): Boolean
}

/** Decodes at the smallest power-of-two step that still covers the target, then scales the rest. */
object AndroidEsdeImageScaler : EsdeImageScaler {
    override fun scale(source: File, target: File, maxWidth: Int, maxHeight: Int, keepAlpha: Boolean): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth && bounds.outHeight / (sample * 2) >= maxHeight) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return false
        val scale = minOf(1f, maxWidth / decoded.width.toFloat(), maxHeight / decoded.height.toFloat())
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            decoded
        }
        return try {
            target.outputStream().use {
                val format = if (keepAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                sized.compress(format, JPEG_QUALITY, it)
            }
        } finally {
            if (sized !== decoded) sized.recycle()
            decoded.recycle()
        }
    }
}

/** Copied files for one system, relative to the theme folder. */
data class EsdeSystemArtFiles(val logo: String? = null, val background: String? = null)

/** Copied fonts by role, relative to the theme folder. */
data class EsdeFontFiles(val display: String? = null, val body: String? = null)

/**
 * Copies only what the player's systems use from an ES-DE theme into [outDir], the Jarn theme's
 * own folder: pictures shrunk to [sizes], fonts that pass the glyph check, and nothing past
 * [capBytes]. Never writes into [themeDir], and never reads outside it. Blocking file IO.
 */
class EsdeAssetCopier(
    private val themeDir: File,
    private val outDir: File,
    private val capBytes: Long = ESDE_ASSET_CAP_BYTES,
    private val sizes: EsdeAssetSizes = EsdeAssetSizes(),
    private val scaler: EsdeImageScaler = AndroidEsdeImageScaler,
) {
    private val root = themeDir.canonicalFile
    private var spent = 0L
    private val notes = mutableListOf<EsdeFallbackNote>()
    val copied = mutableListOf<String>()

    fun takeNotes(): List<EsdeFallbackNote> = notes.toList().also { notes.clear() }

    fun copyArt(art: List<EsdeSystemArtCandidates>): Map<String, EsdeSystemArtFiles> = art.associate { system ->
        val logo = copyImage(system.logo, "$ASSET_DIR/logos/${system.jarnSystemId}.png", sizes.logo, keepAlpha = true, system.jarnSystemId)
        val background = copyImage(system.background, "$ASSET_DIR/backgrounds/${system.jarnSystemId}.jpg", sizes.background, keepAlpha = false, system.jarnSystemId)
        if (logo == null && system.logo.isNotEmpty()) notes += EsdeFallbackNote.LogoNotCopied(system.jarnSystemId)
        if (background == null && system.background.isNotEmpty()) notes += EsdeFallbackNote.BackgroundNotCopied(system.jarnSystemId)
        system.jarnSystemId to EsdeSystemArtFiles(logo, background)
    }.filterValues { it.logo != null || it.background != null }

    /** The first font per role that is a real TrueType or OpenType file with Latin glyphs. */
    fun copyFonts(fonts: List<EsdeFontCandidate>): EsdeFontFiles {
        val picked = EsdeFontRole.entries.associateWith { role ->
            fonts.filter { it.role == role }.firstNotNullOfOrNull { candidate -> copyFont(candidate) }
        }
        return EsdeFontFiles(display = picked[EsdeFontRole.Display], body = picked[EsdeFontRole.Body])
    }

    private fun copyFont(candidate: EsdeFontCandidate): String? {
        val source = inside(candidate.path) ?: return null
        val ext = source.extension.lowercase()
        val bytes = source.takeIf { ext in FONT_EXTENSIONS && it.length() <= MAX_FONT_BYTES }?.readBytes()
        if (bytes == null || !isUsableFont(bytes)) {
            notes += EsdeFallbackNote.FontRefused(candidate.path)
            return null
        }
        val relative = "$ASSET_DIR/fonts/${candidate.role.name.lowercase()}.$ext"
        return relative.takeIf { place(relative) { file -> file.writeBytes(bytes); true } }
    }

    private fun copyImage(candidates: List<String>, relative: String, size: EsdeAssetSizes.Size, keepAlpha: Boolean, systemId: String): String? {
        val source = candidates.asSequence().mapNotNull(::inside).firstOrNull() ?: return null
        val ext = source.extension.lowercase()
        if (ext == "svg") {
            notes += EsdeFallbackNote.SvgNotCopied(systemId)
            return null
        }
        if (ext !in IMAGE_EXTENSIONS || !isImageWithinCaps(readImageSize(source.header()))) return null
        return relative.takeIf { place(relative) { scaler.scale(source, it, size.width, size.height, keepAlpha) } }
    }

    /** Writes one file, and takes it back out when it fails or would pass the cap. */
    private fun place(relative: String, write: (File) -> Boolean): Boolean {
        if (spent >= capBytes) {
            notes += EsdeFallbackNote.AssetsCapped
            return false
        }
        val target = File(outDir, relative)
        target.parentFile?.mkdirs()
        val ok = try {
            write(target) && target.isFile
        } catch (e: IOException) {
            false
        }
        if (!ok || spent + target.length() > capBytes) {
            if (ok) {
                notes += EsdeFallbackNote.AssetsCapped
                spent = capBytes
            }
            target.delete()
            return false
        }
        spent += target.length()
        copied += relative
        return true
    }

    /** A theme-relative path as an existing file inside the theme, or null. */
    private fun inside(path: String): File? {
        val file = File(root, path.removePrefix("./")).canonicalFile
        return file.takeIf { it.isFile && it.toPath().startsWith(root.toPath()) }
    }
}

private fun File.header(): ByteArray = inputStream().use { input ->
    val bytes = ByteArray(HEADER_BYTES)
    var filled = 0
    while (filled < bytes.size) {
        val read = input.read(bytes, filled, bytes.size - filled)
        if (read < 0) break
        filled += read
    }
    bytes.copyOf(filled)
}
