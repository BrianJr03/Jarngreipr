package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnTheme

private class JarnLibraryLookProvider : PreviewParameterProvider<JarnLook> {
    override val values: Sequence<JarnLook> = JarnLooks.All.asSequence()
}

@Composable
private fun JarnLibraryPreviewFrame(look: JarnLook, content: @Composable ColumnScope.() -> Unit) {
    JarnTheme(look = look) {
        Column(
            modifier = Modifier
                .background(JarnTheme.colors.bg)
                .padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
            content = content,
        )
    }
}

@Composable
private fun JarnPreviewArt() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFF5CC8F5)))),
    )
}

@Preview(widthDp = 538)
@Composable
private fun JarnLibraryTilesPreview(@PreviewParameter(JarnLibraryLookProvider::class) look: JarnLook) {
    val title = stringResource(R.string.jarn_preview_game_title)
    JarnLibraryPreviewFrame(look) {
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnSystemTile(
                title = stringResource(R.string.jarn_preview_system_title),
                subtitle = stringResource(R.string.jarn_preview_system_count),
                onClick = {},
                modifier = Modifier.width(136.dp),
            ) { JarnPreviewArt() }
            JarnGameTile(title, {}, Modifier.width(112.dp)) { JarnPreviewArt() }
            JarnGameTile(title, {}, Modifier.width(112.dp)) { JarnArtPlaceholder(title) }
        }
    }
}

@Preview(widthDp = 833, heightDp = 468)
@Preview(widthDp = 833, heightDp = 468, fontScale = 1.3f)
@Composable
private fun JarnHeroPanelPreview(@PreviewParameter(JarnLibraryLookProvider::class) look: JarnLook) {
    JarnTheme(look = look) {
        JarnHeroPanel(
            eyebrow = stringResource(R.string.jarn_preview_system_title),
            title = stringResource(R.string.jarn_preview_game_title),
            body = stringResource(R.string.jarn_preview_game_body),
            modifier = Modifier.fillMaxWidth().height(468.dp),
            art = { JarnPreviewArt() },
        )
    }
}

@Preview(widthDp = 538)
@Composable
private fun JarnMetadataPlatePreview(@PreviewParameter(JarnLibraryLookProvider::class) look: JarnLook) {
    JarnLibraryPreviewFrame(look) {
        JarnMetadataPlate(
            entries = listOf(
                stringResource(R.string.jarn_preview_meta_genre) to stringResource(R.string.jarn_preview_meta_genre_value),
                stringResource(R.string.jarn_preview_meta_players) to stringResource(R.string.jarn_preview_meta_players_value),
                stringResource(R.string.jarn_preview_meta_released) to stringResource(R.string.jarn_preview_meta_released_value),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
