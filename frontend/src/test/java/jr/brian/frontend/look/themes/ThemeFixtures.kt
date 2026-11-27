package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemeTokens
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal val testColors = ThemeColors(
    bg = ThemeColor(0xFF000000.toInt()),
    surface = ThemeColor(0xFF111111.toInt()),
    raised = ThemeColor(0xFF222222.toInt()),
    textHigh = ThemeColor(0xFFFFFFFF.toInt()),
    textMid = ThemeColor(0xFFDDDDDD.toInt()),
    textLow = ThemeColor(0xFFBBBBBB.toInt()),
    accent = ThemeColor(0xFFFF8800.toInt()),
    danger = ThemeColor(0xFFFF4444.toInt()),
    success = ThemeColor(0xFF44FF44.toInt()),
)

internal fun testSpec(name: String = "Art Book") = JarnThemeSpec(meta = ThemeMeta(name = name), tokens = ThemeTokens(testColors))

internal fun themeJson(spec: JarnThemeSpec = testSpec()) = JarnThemeFormat.encode(spec).toByteArray()

/** Just enough PNG for the header check: signature and IHDR size. */
internal fun pngHeader(width: Int, height: Int): ByteArray = ByteBuffer.allocate(33).order(ByteOrder.BIG_ENDIAN).apply {
    put(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
    putInt(13)
    put("IHDR".toByteArray())
    putInt(width)
    putInt(height)
}.array()

/** A one-table TrueType file whose format 4 cmap maps [first]..[last]. */
internal fun ttf(first: Int = 'A'.code, last: Int = 'Z'.code): ByteArray {
    val buf = ByteBuffer.allocate(28 + 12 + 32).order(ByteOrder.BIG_ENDIAN)
    buf.putInt(0x00010000).putShort(1).putShort(16).putShort(0).putShort(0)
    buf.putInt(0x636D6170).putInt(0).putInt(28).putInt(44)
    buf.putShort(0).putShort(1).putShort(3).putShort(1).putInt(12)
    buf.putShort(4).putShort(32).putShort(0).putShort(4).putShort(4).putShort(1).putShort(0)
    buf.putShort(last.toShort()).putShort(0xFFFF.toShort()).putShort(0)
    buf.putShort(first.toShort()).putShort(0xFFFF.toShort())
    buf.putShort(1).putShort(1).putShort(0).putShort(0)
    return buf.array()
}

internal fun zipOf(file: File, vararg entries: Pair<String, ByteArray>): File {
    ZipOutputStream(file.outputStream()).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return file
}

/** Rewrites central directory fields of [name]: the made-by host, the external attributes, or the size. */
internal fun patchCentral(zip: File, name: String, madeByHost: Int? = null, unixMode: Int? = null, size: Int? = null) {
    val bytes = zip.readBytes()
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    var at = 0
    while (at + 46 <= bytes.size) {
        if (buf.getInt(at) == 0x02014b50) {
            val nameLength = buf.getShort(at + 28).toInt() and 0xFFFF
            if (String(bytes, at + 46, nameLength) == name) {
                RandomAccessFile(zip, "rw").use { raf ->
                    madeByHost?.let { raf.seek(at + 5L); raf.write(it) }
                    unixMode?.let { mode ->
                        raf.seek(at + 38L)
                        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(mode shl 16).array())
                    }
                    size?.let { s ->
                        raf.seek(at + 24L)
                        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(s).array())
                    }
                }
                return
            }
        }
        at++
    }
    error("No central record for $name")
}
