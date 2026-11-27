package jr.brian.jarnfe.themekit

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeDateFormat
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.nowDateText
import jr.brian.jarnfe.library.nowPlaytimeText
import jr.brian.jarnfe.library.stillImageBuilder

internal const val THEME_TEXT_TAG = "theme_text_"
private val PanelOutlineWidth = 1.dp

/** A focused-item field, or fixed text, on a readable plate. Hides when the field is empty. */
@Composable
internal fun ThemeTextLayer(layer: ThemeLayer.Text, focus: FocusedItem) {
    val value = resolveThemeText(layer.content, focus) ?: return
    ThemePlatedText(layer.id, value.asString(), layer.style, layer.color, layer.align, layer.verticalAlign, layer.maxLines)
}

@Composable
internal fun ThemeDateLayer(layer: ThemeLayer.Date, focus: FocusedItem) {
    val value = resolveThemeDate(layer.field, focus) ?: return
    ThemePlatedText(layer.id, themeDateText(value, layer.format), layer.style, layer.color, layer.align, layer.verticalAlign, maxLines = 1)
}

@Composable
internal fun ThemeRatingLayer(layer: ThemeLayer.Rating, focus: FocusedItem) {
    val rating = resolveThemeRating(focus) ?: return
    ThemeReadablePlate(ThemeAlign.Start) {
        ThemeRatingStars(rating, layer.color.textColor(JarnTheme.colors))
    }
}

/** A filled plate, or only its outline when [ThemeLayer.Panel.outlined] is set. */
@Composable
internal fun ThemePanelLayer(layer: ThemeLayer.Panel) {
    val shape = layer.corner.shape()
    val color = layer.color.color(JarnTheme.colors)
    val drawn = if (layer.outlined) Modifier.border(PanelOutlineWidth, color, shape) else Modifier.background(color, shape)
    Box(Modifier.fillMaxSize().then(drawn))
}

/**
 * The first of [paths] that loads, decoded at most at [decodeCap]. A file that fails moves on to
 * the next, and when none load the layer hides rather than showing a broken image.
 */
@Composable
internal fun ThemePictureLayer(paths: List<String>, fit: ThemeFit, corner: ThemeCorner, decodeCap: ThemePxSize?) {
    var index by remember(paths) { mutableIntStateOf(0) }
    val path = paths.getOrNull(index) ?: return
    val context = LocalContext.current
    val request = remember(context, path, decodeCap) {
        stillImageBuilder(context, path)
            .apply { decodeCap?.let { size(it.width, it.height) } }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().clip(corner.shape()),
        contentScale = fit.contentScale,
        onError = { index++ },
    )
}

@Composable
private fun ThemePlatedText(
    id: String,
    text: String,
    style: ThemeTextRole,
    color: ThemeColorRole,
    align: ThemeAlign,
    verticalAlign: ThemeAlign,
    maxLines: Int,
) {
    ThemeReadablePlate(align, verticalAlign) {
        JarnText(
            text = text,
            style = style.style(),
            color = color.textColor(JarnTheme.colors),
            modifier = Modifier.testTag(THEME_TEXT_TAG + id),
            maxLines = maxLines.coerceAtLeast(1),
            textAlign = align.textAlign,
        )
    }
}

/**
 * The art scrim from Jarn's color repair, which keeps text readable over any art behind it. In a
 * stack the plate only takes the height it needs, so the rows below can move up.
 */
@Composable
internal fun ThemeReadablePlate(
    align: ThemeAlign,
    verticalAlign: ThemeAlign = ThemeAlign.Center,
    content: @Composable () -> Unit,
) {
    val area = if (LocalThemeStacked.current) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
    Box(area, contentAlignment = plateAlignment(align, verticalAlign)) {
        Box(
            Modifier
                .background(JarnTheme.colors.artScrim, JarnTheme.radius.smShape)
                .padding(horizontal = JarnTheme.space.sm, vertical = JarnTheme.space.xs),
        ) { content() }
    }
}

@Composable
internal fun ThemeTextValue.asString(): String = when (this) {
    is ThemeTextValue.Plain -> text
    is ThemeTextValue.GameCount -> pluralStringResource(R.plurals.jarnfe_library_game_count, count, count)
    is ThemeTextValue.FavoriteCount -> pluralStringResource(R.plurals.jarnfe_theme_favorite_count, count, count)
    is ThemeTextValue.PlayCount -> pluralStringResource(R.plurals.jarnfe_resume_plays, count, count)
    is ThemeTextValue.Playtime -> nowPlaytimeText(minutes)
}

@Composable
private fun themeDateText(value: ThemeDateValue, format: ThemeDateFormat): String {
    val epochMs = value.epochMs ?: return value.year.toString()
    return when (format) {
        ThemeDateFormat.Year -> value.year.toString()
        ThemeDateFormat.Short -> nowDateText(epochMs)
        ThemeDateFormat.Relative -> remember(epochMs) {
            DateUtils.getRelativeTimeSpanString(epochMs, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
        }
    }
}
