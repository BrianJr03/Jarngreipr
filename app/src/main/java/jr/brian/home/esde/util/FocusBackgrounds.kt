package jr.brian.home.esde.util

import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.model.GameInfo

/**
 * The 16:9-ish artwork best suited to a full-screen backdrop, or null.
 *
 * Fanart first — it is the 16:9 hero in ES-DE's media layout. Miximage is
 * last because it is a composited portrait-ish image that crops badly at
 * full-screen. Blank strings are treated as absent.
 */
fun GameInfo.heroBackgroundPath(): String? =
    imagePathFor(GameImageType.Fanart).nonBlankOrNull()
        ?: imagePathFor(GameImageType.Screenshots).nonBlankOrNull()
        ?: imagePathFor(GameImageType.TitleScreens).nonBlankOrNull()
        ?: imagePathFor(GameImageType.MixImages).nonBlankOrNull()

private fun String?.nonBlankOrNull(): String? =
    this?.takeIf { it.isNotBlank() }
