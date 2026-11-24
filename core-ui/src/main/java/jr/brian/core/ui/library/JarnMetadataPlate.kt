package jr.brian.core.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme

private const val METADATA_COLUMNS = 2

@Composable
fun JarnMetadataPlate(entries: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    JarnSurface(modifier = modifier, shape = JarnTheme.radius.smShape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(JarnTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            entries.chunked(METADATA_COLUMNS).forEach { row -> JarnMetadataRow(row) }
        }
    }
}

@Composable
private fun JarnMetadataRow(row: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        row.forEach { (label, value) ->
            JarnMetadataCell(label, value, Modifier.weight(1f))
        }
        repeat(METADATA_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun JarnMetadataCell(label: String, value: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        JarnText(label, JarnTheme.type.caption, JarnTheme.colors.textLow, maxLines = 1)
        JarnText(value, JarnTheme.type.captionStrong, JarnTheme.colors.textHigh, maxLines = 1)
    }
}
