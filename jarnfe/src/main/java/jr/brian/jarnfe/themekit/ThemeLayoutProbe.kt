package jr.brian.jarnfe.themekit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset

/**
 * Where a theme view was last placed, in root pixels, for tools drawn over it like the editor's
 * selection. Only the editor provides one; browse leaves [LocalThemeLayoutProbe] null.
 */
internal class ThemeLayoutProbe {
    var placed by mutableStateOf<PlacedThemeView?>(null)
    var origin by mutableStateOf(Offset.Zero)
    var screen by mutableStateOf<ThemeScreen?>(null)
}

internal val LocalThemeLayoutProbe = staticCompositionLocalOf<ThemeLayoutProbe?> { null }
