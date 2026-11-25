package jr.brian.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Puts [content] back on Jarn's own type, so `JarnTheme.type` there ignores font overrides.
 * For sheets, dialogs and anything showing names, which can hold glyphs the theme fonts lack.
 */
@Composable
fun JarnBaseTypeScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalJarnType provides JarnTheme.baseType, content = content)
}
