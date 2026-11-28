package jr.brian.jarnfe.themekit

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.core.model.MediaKind

/**
 * Media kinds a preview pretends the focused game doesn't have, such as the editor's No video
 * case. Empty everywhere else. Only what theme layers read changes; the library never does.
 */
internal val LocalThemeHiddenMedia = staticCompositionLocalOf { emptySet<MediaKind>() }

/** [this] without the [hidden] kinds. */
internal fun ThemeFocusMedia.without(hidden: Set<MediaKind>): ThemeFocusMedia =
    if (hidden.isEmpty()) this else copy(paths = paths.filterKeys { it !in hidden })
