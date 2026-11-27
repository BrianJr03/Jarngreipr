package jr.brian.jarnfe.themekit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ViewSpec
import java.io.File

/**
 * What a layer reads. Text reads [live] so it follows focus at once; pictures and video read
 * [settled] and its [media], so scrolling never decodes art. [themeDir] holds the theme's own files.
 */
internal data class ThemeDecorFocus(
    val live: FocusedItem,
    val settled: FocusedItem,
    val media: ThemeFocusMedia,
    val themeDir: File? = null,
)

/**
 * A theme view whose decor layers follow [focused]. The browse screen passes its focus on every
 * change (see [toFocusedItem]) and draws its list in [primary]; the layers never take focus.
 */
@Composable
internal fun JarnThemeDecorView(
    view: ViewSpec,
    focused: FocusedItem,
    primary: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    themeDir: File? = null,
    insets: ThemeInsets = rememberJarnThemeInsets(),
    onIssues: (List<ThemeLayoutIssue>) -> Unit = {},
) {
    val settled = rememberSettledThemeFocus(focused)
    val decor = ThemeDecorFocus(focused, settled, rememberThemeFocusMedia(settled), themeDir)
    JarnThemeLayout(
        view = view,
        primary = primary,
        layer = { placed -> ThemeDecorLayer(placed, decor) },
        modifier = modifier,
        insets = insets,
        onIssues = onIssues,
    )
}

/** One decor layer at its bounds, or nothing when its data is missing or its visibleWhen fails. */
@Composable
internal fun ThemeDecorLayer(placed: PlacedThemeLayer, decor: ThemeDecorFocus) {
    val layer = placed.layer
    if (!themeLayerVisible(layer.visibleWhen, decor.live, decor.media)) return
    val area = if (LocalThemeStacked.current) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
    Box(area.graphicsLayer { alpha = layer.opacity.coerceIn(0f, 1f) }) {
        when (layer) {
            is ThemeLayer.Text -> ThemeTextLayer(layer, decor.live)
            is ThemeLayer.Date -> ThemeDateLayer(layer, decor.live)
            is ThemeLayer.Rating -> ThemeRatingLayer(layer, decor.live)
            is ThemeLayer.Panel -> ThemePanelLayer(layer)
            is ThemeLayer.Picture -> ThemeBoundPicture(layer, decor, placed.decodeCap)
            is ThemeLayer.Video -> ThemeVideoLayer(layer, decor, placed.decodeCap)
        }
    }
}

@Composable
private fun ThemeBoundPicture(layer: ThemeLayer.Picture, decor: ThemeDecorFocus, decodeCap: ThemePxSize?) {
    val paths = resolveThemePictures(listOf(layer.source) + layer.fallbacks, decor.settled, decor.media, decor.themeDir)
    if (paths.isEmpty()) return
    ThemePictureLayer(paths, layer.fit, layer.corner, decodeCap)
}
