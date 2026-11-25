package jr.brian.jarnfe.systems

import androidx.annotation.StringRes
import jr.brian.jarnfe.R

internal enum class SystemSection(@param:StringRes val labelRes: Int) {
    Appearance(R.string.jarnfe_systems_section_appearance),
    Color(R.string.jarnfe_systems_section_color),
    Storage(R.string.jarnfe_systems_section_storage),
    Emulator(R.string.jarnfe_systems_section_emulator),
    Actions(R.string.jarnfe_systems_section_actions),
}

internal enum class SystemFilter(@param:StringRes val labelRes: Int) {
    All(R.string.jarnfe_systems_filter_all),
    Shown(R.string.jarnfe_systems_filter_shown),
    Hidden(R.string.jarnfe_systems_filter_hidden);

    fun matches(system: JarnManagedSystem): Boolean = when (this) {
        All -> true
        Shown -> !system.isHidden
        Hidden -> system.isHidden
    }

    fun next(): SystemFilter = entries[(ordinal + 1) % entries.size]
}

internal sealed interface SystemsPage {
    data object List : SystemsPage
    data class Editor(val systemId: String, val section: SystemSection) : SystemsPage
    data class Emulators(val systemId: String, val allApps: Boolean = false) : SystemsPage
    data class HiddenGames(val systemId: String) : SystemsPage
    data class AddChoose(val treeUri: String) : SystemsPage
    data class AddNoGames(val treeUri: String, val systemId: String) : SystemsPage
}

/** Where the player is in the flow. Back walks up one page, then leaves. */
internal data class SystemsNav(
    val page: SystemsPage = SystemsPage.List,
    val filter: SystemFilter = SystemFilter.All,
    val focusedSystemId: String? = null,
) {
    fun back(): SystemsNav? = when (val current = page) {
        SystemsPage.List -> null
        is SystemsPage.Editor -> copy(page = SystemsPage.List, focusedSystemId = current.systemId)
        is SystemsPage.Emulators ->
            if (current.allApps) {
                copy(page = current.copy(allApps = false))
            } else {
                copy(page = SystemsPage.Editor(current.systemId, SystemSection.Emulator))
            }
        is SystemsPage.HiddenGames -> copy(page = SystemsPage.Editor(current.systemId, SystemSection.Actions))
        is SystemsPage.AddChoose -> copy(page = SystemsPage.List)
        is SystemsPage.AddNoGames -> copy(page = SystemsPage.AddChoose(current.treeUri))
    }

    fun stepSection(delta: Int): SystemsNav {
        val editor = page as? SystemsPage.Editor ?: return this
        val sections = SystemSection.entries
        val index = (editor.section.ordinal + delta).coerceIn(sections.indices)
        return copy(page = editor.copy(section = sections[index]))
    }

    fun openEditor(systemId: String): SystemsNav =
        copy(page = SystemsPage.Editor(systemId, SystemSection.Appearance), focusedSystemId = systemId)

    fun cycleFilter(): SystemsNav = copy(filter = filter.next(), focusedSystemId = null)

    companion object {
        fun opening(systemId: String?): SystemsNav =
            systemId?.let { SystemsNav().openEditor(it) } ?: SystemsNav()
    }
}

/** Hue, saturation, brightness, and opacity, matching how Classic stores a system color. */
internal data class SystemColor(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val brightness: Float = 1f,
    val alpha: Float = 1f,
) {
    fun toArgb(): Long {
        val h = hue.coerceIn(0f, 360f).let { if (it == 360f) 0f else it } / 60f
        val chroma = brightness.coerceIn(0f, 1f) * saturation.coerceIn(0f, 1f)
        val x = chroma * (1f - kotlin.math.abs(h % 2f - 1f))
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0f)
            1 -> Triple(x, chroma, 0f)
            2 -> Triple(0f, chroma, x)
            3 -> Triple(0f, x, chroma)
            4 -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        val m = brightness.coerceIn(0f, 1f) - chroma
        return (channel(alpha) shl 24) or (channel(r + m) shl 16) or (channel(g + m) shl 8) or channel(b + m)
    }

    companion object {
        private fun channel(value: Float): Long = (value.coerceIn(0f, 1f) * 255f + 0.5f).toLong()

        fun fromArgb(argb: Long?): SystemColor {
            if (argb == null) return SystemColor()
            if (argb == 0L) return SystemColor(alpha = 0f)
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val delta = max - minOf(r, g, b)
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }.let { if (it < 0f) it + 360f else it }
            return SystemColor(
                hue = hue,
                saturation = if (max == 0f) 0f else delta / max,
                brightness = max,
                alpha = ((argb shr 24) and 0xFF) / 255f,
            )
        }
    }
}
