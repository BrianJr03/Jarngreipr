package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme

private const val SYSTEM_TILE_ASPECT = 136f / 98f
private const val GAME_TILE_ASPECT = 4f / 5f

internal val JarnTileCaptionScrim = Color(0xFF080706).copy(alpha = 0.82f)
internal val JarnTileCaptionTitle = Color.White
internal val JarnTileCaptionSubtitle = Color(0xFFE5DED8)

@Composable
fun JarnSystemTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit,
) {
    JarnLibraryTileFrame(
        shape = JarnTheme.tileShape,
        aspectRatio = SYSTEM_TILE_ASPECT,
        captioned = true,
        description = null,
        onClick = onClick,
        modifier = modifier,
        art = art,
    ) {
        JarnTileCaption(title, subtitle, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun JarnGameTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = GAME_TILE_ASPECT,
    art: @Composable BoxScope.() -> Unit,
) {
    JarnLibraryTileFrame(
        shape = JarnTheme.look.tileShape ?: JarnTheme.radius.smShape,
        aspectRatio = aspectRatio,
        captioned = false,
        description = title,
        onClick = onClick,
        modifier = modifier,
        art = art,
    )
}

@Composable
private fun JarnLibraryTileFrame(
    shape: Shape,
    aspectRatio: Float,
    captioned: Boolean,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    art: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .semantics { description?.let { contentDescription = it } }
            .jarnInteractive(shape, source, onClick = onClick)
            .clip(shape)
            .background(JarnTheme.colors.raised),
    ) {
        JarnArtSlot(captioned, art)
        overlay()
    }
}

@Composable
private fun JarnTileCaption(title: String, subtitle: String?, modifier: Modifier) {
    val space = JarnTheme.space
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(space.sm)
                .background(Brush.verticalGradient(listOf(Color.Transparent, JarnTileCaptionScrim))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(JarnTileCaptionScrim)
                .padding(start = space.sm, end = space.sm, bottom = space.xs),
        ) {
            JarnText(title, JarnTheme.type.captionStrong, JarnTileCaptionTitle, maxLines = 1)
            subtitle?.let {
                JarnText(it, JarnTheme.type.caption, JarnTileCaptionSubtitle, maxLines = 1)
            }
        }
    }
}
