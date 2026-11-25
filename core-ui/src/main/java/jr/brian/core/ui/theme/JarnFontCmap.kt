package jr.brian.core.ui.theme

import java.nio.ByteBuffer

private const val CMAP_TAG = 0x636D6170
private const val TABLE_RECORD_BYTES = 16
private const val ENCODING_RECORD_BYTES = 8

/**
 * The code points a TrueType/OpenType font maps, read from its cmap table (formats 4 and 12).
 * Paint.hasGlyph also counts system fallback fonts, so it cannot tell a Latin subset from a full face.
 */
internal fun jarnCmapCoverage(bytes: ByteArray): (Int) -> Boolean {
    val buf = ByteBuffer.wrap(bytes)
    val tables = buf.getShort(4).toInt() and 0xFFFF
    val cmap = (0 until tables)
        .map { 12 + it * TABLE_RECORD_BYTES }
        .firstOrNull { buf.getInt(it) == CMAP_TAG }
        ?.let { buf.getInt(it + 8) } ?: return { false }
    val encodings = buf.getShort(cmap + 2).toInt() and 0xFFFF
    val subtables = (0 until encodings).map { cmap + buf.getInt(cmap + 4 + it * ENCODING_RECORD_BYTES + 4) }
    val ranges = subtables.firstOrNull { buf.getShort(it).toInt() == 12 }?.let { format12(buf, it) }
        ?: subtables.firstOrNull { buf.getShort(it).toInt() == 4 }?.let { format4(buf, it) }
        ?: return { false }
    return { cp -> ranges.any { cp in it } }
}

private fun format12(buf: ByteBuffer, at: Int): List<IntRange> {
    val groups = buf.getInt(at + 12)
    return (0 until groups).map {
        val group = at + 16 + it * 12
        buf.getInt(group)..buf.getInt(group + 4)
    }
}

private fun format4(buf: ByteBuffer, at: Int): List<IntRange> {
    val segments = (buf.getShort(at + 6).toInt() and 0xFFFF) / 2
    val ends = at + 14
    val starts = ends + segments * 2 + 2
    val deltas = starts + segments * 2
    val offsets = deltas + segments * 2
    return (0 until segments).flatMap { seg ->
        val end = buf.getShort(ends + seg * 2).toInt() and 0xFFFF
        val start = buf.getShort(starts + seg * 2).toInt() and 0xFFFF
        val delta = buf.getShort(deltas + seg * 2).toInt()
        val rangeOffset = buf.getShort(offsets + seg * 2).toInt() and 0xFFFF
        if (end == 0xFFFF && start == 0xFFFF) return@flatMap emptyList()
        if (rangeOffset == 0) return@flatMap listOf(start..end)
        (start..end).filter { cp ->
            val glyphAt = offsets + seg * 2 + rangeOffset + (cp - start) * 2
            glyphAt + 1 < buf.limit() && ((buf.getShort(glyphAt).toInt() + delta) and 0xFFFF) != 0 &&
                buf.getShort(glyphAt).toInt() != 0
        }.map { it..it }
    }
}
