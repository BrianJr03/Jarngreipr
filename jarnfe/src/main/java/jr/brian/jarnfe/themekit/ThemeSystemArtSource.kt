package jr.brian.jarnfe.themekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.jarnfe.library.JarnLibrarySystem
import java.io.File

/**
 * The active theme's own art per system. A system the theme covers shows only the theme's
 * files: its logo, its background as the card art and Now backdrop, and the name where either is
 * missing, so a card never mixes the theme's art with Jarn's. Other systems keep Jarn's art.
 */
@Immutable
internal class ThemeSystemArtSource(private val art: Map<String, ThemeSystemArt>, private val themeDir: File?) {

    fun apply(system: JarnLibrarySystem): JarnLibrarySystem {
        val entry = art[system.id] ?: return system
        val logo = entry.logo?.let { themeFile(themeDir, it) }
        val background = entry.background?.let { themeFile(themeDir, it) }
        if (logo == null && background == null) return system
        return system.copy(artPath = background, logoPath = logo, nowBackgroundPath = background)
    }

    /** [focus] with its system, or the focused game's system, showing the theme's art. */
    fun apply(focus: FocusedItem): FocusedItem = when (focus) {
        FocusedItem.None -> focus
        is FocusedItem.System -> FocusedItem.System(apply(focus.system))
        is FocusedItem.Game -> focus.copy(system = focus.system?.let(::apply))
    }

    override fun equals(other: Any?): Boolean =
        other is ThemeSystemArtSource && other.art == art && other.themeDir == themeDir

    override fun hashCode(): Int = 31 * art.hashCode() + (themeDir?.hashCode() ?: 0)

    companion object {
        val None = ThemeSystemArtSource(emptyMap(), null)
    }
}

/** The active theme's per-system art. Only the shell root provides it, from the theme on screen. */
internal val LocalThemeSystemArt = staticCompositionLocalOf { ThemeSystemArtSource.None }

/** This system as the active theme shows it. */
@Composable
internal fun JarnLibrarySystem.withThemeArt(): JarnLibrarySystem = LocalThemeSystemArt.current.apply(this)
