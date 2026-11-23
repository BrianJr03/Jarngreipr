package jr.brian.jarnfe.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasKeyboardSheet
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.canvas.CanvasSheetListMaxHeight

private const val PACK_ICON_COLUMNS = 6
private val PackIconSize = 40.dp

/** A null pick means no pack. It is only offered when [offerNone] is true. */
@Composable
internal fun JarnIconPackListSheet(
    data: JarnAppOptionsData,
    offerNone: Boolean,
    onPick: (JarnIconPack?) -> Unit,
    onClose: () -> Unit,
) {
    val packs by produceState<List<JarnIconPack>?>(null, data) { value = data.iconPacks() }
    val first = remember { FocusRequester() }
    val loaded = packs
    val hasRows = offerNone || !loaded.isNullOrEmpty()
    LaunchedEffect(loaded) {
        if (!offerNone && !loaded.isNullOrEmpty()) {
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }
    CanvasModalSheet(stringResource(R.string.jarnfe_app_icon_packs_title), first.takeIf { hasRows }, onClose) {
        val scroll = rememberScrollState()
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight)
                    .verticalScroll(scroll)
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                if (offerNone) {
                    JarnNavigateRow(
                        label = stringResource(R.string.jarnfe_app_icon_pack_none),
                        onClick = { onPick(null) },
                        modifier = Modifier.fillMaxWidth().focusRequester(first),
                    )
                }
                loaded?.forEachIndexed { index, pack ->
                    val focus = if (!offerNone && index == 0) Modifier.focusRequester(first) else Modifier
                    JarnNavigateRow(
                        label = pack.name,
                        onClick = { onPick(pack) },
                        modifier = Modifier.fillMaxWidth().then(focus),
                    )
                }
                IconPackStatus(loading = loaded == null, empty = loaded?.isEmpty() == true)
            }
        }
    }
}

@Composable
private fun IconPackStatus(loading: Boolean, empty: Boolean) {
    val message = when {
        loading -> R.string.jarnfe_app_icon_loading
        empty -> R.string.jarnfe_app_icon_packs_empty
        else -> return
    }
    JarnText(stringResource(message), JarnTheme.type.body, JarnTheme.colors.textMid)
}

/** Icons load as they scroll into view, because a pack can hold thousands. */
@Composable
internal fun JarnIconPackIconsSheet(
    data: JarnAppOptionsData,
    pack: JarnIconPack,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember(pack.packageName) { mutableStateOf("") }
    var searching by remember(pack.packageName) { mutableStateOf(false) }
    val names by produceState<List<String>?>(null, data, pack.packageName) {
        value = data.iconPackIconNames(pack.packageName)
    }
    if (searching) {
        CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_app_icon_search),
            onDone = {
                query = it.trim()
                searching = false
            },
            onCancel = { searching = false },
            initialText = query,
        )
        return
    }
    val loaded = names
    val rows = remember(loaded, query) {
        loaded?.let { jarnFilterIconNames(it, query).chunked(PACK_ICON_COLUMNS) }
    }
    val first = remember { FocusRequester() }
    val listState = rememberLazyListState()
    CanvasModalSheet(pack.name, first, onClose) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_app_icon_search),
            supporting = query.ifEmpty { null },
            onClick = { searching = true },
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        if (rows.isNullOrEmpty()) {
            PackIconsStatus(loading = rows == null)
        } else {
            JarnRevealLazyItems(listState) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = CanvasSheetListMaxHeight),
                    contentPadding = PaddingValues(vertical = JarnTheme.space.focusEdge),
                    verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
                ) {
                    items(rows, key = { it.first() }) { row ->
                        PackIconRow(data, pack.packageName, row, onPick)
                    }
                }
            }
        }
    }
}

@Composable
private fun PackIconsStatus(loading: Boolean) {
    val message = if (loading) R.string.jarnfe_app_icon_loading else R.string.jarnfe_app_icon_no_results
    JarnText(stringResource(message), JarnTheme.type.body, JarnTheme.colors.textMid)
}

@Composable
private fun PackIconRow(
    data: JarnAppOptionsData,
    iconPack: String,
    names: List<String>,
    onPick: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        names.forEach { name ->
            PackIconCell(data, iconPack, name, { onPick(name) }, Modifier.weight(1f))
        }
        repeat(PACK_ICON_COLUMNS - names.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun PackIconCell(
    data: JarnAppOptionsData,
    iconPack: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon by produceState<Drawable?>(null, iconPack, name) { value = data.iconPackIcon(iconPack, name) }
    val shape = JarnTheme.radius.mdShape
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = name }
            .jarnInteractive(shape, source, onClick = onClick)
            .background(JarnTheme.colors.raised, shape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(PackIconSize))
    }
}
