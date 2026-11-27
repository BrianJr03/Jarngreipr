package jr.brian.frontend.look.themes

import jr.brian.core.ui.theme.jarnCmapCoverage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal const val THEME_FILE = "theme.json"
internal const val MAX_THEME_JSON_BYTES = 1024 * 1024
private const val HEADER_BYTES = 64 * 1024
private const val MAX_IMAGE_SIDE = 8192
private const val MAX_IMAGE_PIXELS = 32L * 1024 * 1024
private const val FONT_HEADER_BYTES = 12
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "gif")
private val FONT_EXTENSIONS = setOf("ttf", "otf")
private val LATIN_CAPITALS = 'A'.code..'Z'.code

internal enum class ThemeAssetKind { Image, Font }

private const val SVG_EXTENSION = "svg"

/** What a theme file holds, from its name. Anything else is never extracted, so nothing can point at it. */
internal fun themeAssetKind(path: String): ThemeAssetKind? = when (path.substringAfterLast('.', "").lowercase()) {
    in IMAGE_EXTENSIONS, SVG_EXTENSION -> ThemeAssetKind.Image
    in FONT_EXTENSIONS -> ThemeAssetKind.Font
    else -> null
}

internal data class ImageSize(val width: Int, val height: Int)

/** Width and height from a PNG, GIF, WebP or JPEG header, without decoding. Null when unreadable. */
internal fun readImageSize(header: ByteArray): ImageSize? = runCatching {
    val buf = ByteBuffer.wrap(header)
    when {
        header.startsWith(0x89, 'P'.code, 'N'.code, 'G'.code) ->
            ImageSize(buf.order(ByteOrder.BIG_ENDIAN).getInt(16), buf.getInt(20))
        header.startsWith('G'.code, 'I'.code, 'F'.code) -> buf.order(ByteOrder.LITTLE_ENDIAN).let {
            ImageSize(it.getShort(6).toInt() and 0xFFFF, it.getShort(8).toInt() and 0xFFFF)
        }
        header.startsWith('R'.code, 'I'.code, 'F'.code, 'F'.code) -> webpSize(buf.order(ByteOrder.LITTLE_ENDIAN))
        header.startsWith(0xFF, 0xD8) -> jpegSize(buf.order(ByteOrder.BIG_ENDIAN))
        else -> null
    }
}.getOrNull()

private fun ByteArray.startsWith(vararg bytes: Int) =
    size >= bytes.size && bytes.indices.all { (this[it].toInt() and 0xFF) == bytes[it] }

private fun webpSize(buf: ByteBuffer): ImageSize? {
    val chunk = String(ByteArray(4).also { buf.get(12, it) }, Charsets.US_ASCII)
    return when (chunk) {
        "VP8 " -> ImageSize(buf.getShort(26).toInt() and 0x3FFF, buf.getShort(28).toInt() and 0x3FFF)
        "VP8L" -> buf.getInt(21).let { bits -> ImageSize((bits and 0x3FFF) + 1, ((bits ushr 14) and 0x3FFF) + 1) }
        "VP8X" -> ImageSize(uint24(buf, 24) + 1, uint24(buf, 27) + 1)
        else -> null
    }
}

private fun uint24(buf: ByteBuffer, at: Int) =
    (buf.get(at).toInt() and 0xFF) or ((buf.get(at + 1).toInt() and 0xFF) shl 8) or ((buf.get(at + 2).toInt() and 0xFF) shl 16)

/** Walks the JPEG segments to the first start-of-frame marker. */
private fun jpegSize(buf: ByteBuffer): ImageSize? {
    var at = 2
    while (at + 9 < buf.limit()) {
        if (buf.get(at).toInt() and 0xFF != 0xFF) return null
        val marker = buf.get(at + 1).toInt() and 0xFF
        val isFrame = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
        if (isFrame) return ImageSize(buf.getShort(at + 7).toInt() and 0xFFFF, buf.getShort(at + 5).toInt() and 0xFFFF)
        at += 2 + (buf.getShort(at + 2).toInt() and 0xFFFF)
    }
    return null
}

/** An image Jarn can decode safely: a known header within the side and pixel caps. */
internal fun isImageWithinCaps(size: ImageSize?): Boolean =
    size != null && size.width in 1..MAX_IMAGE_SIDE && size.height in 1..MAX_IMAGE_SIDE &&
        size.width.toLong() * size.height <= MAX_IMAGE_PIXELS

/** A TrueType or OpenType file whose cmap maps at least the Latin capitals. */
internal fun isUsableFont(bytes: ByteArray): Boolean {
    if (bytes.size < FONT_HEADER_BYTES) return false
    val tag = ByteBuffer.wrap(bytes).getInt(0)
    val knownTag = tag == 0x00010000 || tag == 0x4F54544F || tag == 0x74727565
    if (!knownTag) return false
    val covers = runCatching { jarnCmapCoverage(bytes) }.getOrNull() ?: return false
    return runCatching { LATIN_CAPITALS.all(covers) }.getOrDefault(false)
}

/**
 * Deletes every extracted file that fails its check, and returns the paths that passed, by kind.
 * Images are only read up to their header, so a huge bitmap is never decoded.
 */
internal fun checkThemeAssets(root: File): Map<String, ThemeAssetKind> {
    val passed = mutableMapOf<String, ThemeAssetKind>()
    root.walkTopDown().filter { it.isFile }.forEach { file ->
        val relative = file.relativeTo(root).invariantSeparatorsPath
        if (relative == THEME_FILE) return@forEach
        val ok = when (themeAssetKind(relative)) {
            ThemeAssetKind.Image -> if (file.isSvg) isSafeThemeSvgFile(file) else isImageWithinCaps(readImageSize(file.readHeader()))
            ThemeAssetKind.Font -> isUsableFont(file.readBytes())
            null -> false
        }
        if (ok) passed[relative] = themeAssetKind(relative)!! else file.delete()
    }
    return passed
}

private fun File.readHeader(): ByteArray = inputStream().use { input ->
    val bytes = ByteArray(HEADER_BYTES)
    var filled = 0
    while (filled < bytes.size) {
        val read = input.read(bytes, filled, bytes.size - filled)
        if (read < 0) break
        filled += read
    }
    bytes.copyOf(filled)
}

internal val File.isSvg: Boolean get() = extension.equals(SVG_EXTENSION, ignoreCase = true)

/** Checks the size first, so an oversized SVG is refused without reading it. */
internal fun isSafeThemeSvgFile(file: File): Boolean =
    file.length() <= MAX_THEME_SVG_BYTES && isSafeThemeSvg(file.readBytes())
