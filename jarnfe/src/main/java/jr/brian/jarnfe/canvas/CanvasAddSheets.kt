package jr.brian.jarnfe.canvas

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices

/** The nine tile types, alphabetical by label as in Classic. */
internal enum class CanvasAddChoice(@param:StringRes val label: Int) {
    App(R.string.jarnfe_canvas_add_app),
    Folder(R.string.jarnfe_canvas_add_folder),
    Art(R.string.jarnfe_canvas_add_art),
    LastPlayed(R.string.jarnfe_canvas_add_last_played),
    Photo(R.string.jarnfe_canvas_add_photo),
    Rom(R.string.jarnfe_canvas_add_rom),
    RssLauncher(R.string.jarnfe_canvas_add_rss),
    RssMusic(R.string.jarnfe_canvas_add_music),
    Widget(R.string.jarnfe_canvas_add_widget),
}

@Composable
internal fun CanvasAddSheet(addingHere: Boolean, onChoice: (CanvasAddChoice) -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    val title = if (addingHere) R.string.jarnfe_canvas_add_here_title else R.string.jarnfe_canvas_add_title
    CanvasModalSheet(stringResource(title), first, onClose) {
        CanvasPickerColumn {
            CanvasAddChoice.entries.forEachIndexed { index, choice ->
                JarnNavigateRow(
                    label = stringResource(choice.label),
                    onClick = { onChoice(choice) },
                    modifier = Modifier.fillMaxWidth().then(
                        if (index == 0) Modifier.focusRequester(first) else Modifier,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CanvasPickerColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = CanvasSheetListMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(vertical = JarnTheme.space.focusEdge),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) { content() }
}

/** Loads [load] once, and shows a loading or empty line until there are rows to focus. */
@Composable
private fun <T> CanvasLoadedPicker(
    title: String,
    load: suspend () -> List<T>,
    onClose: () -> Unit,
    footer: @Composable () -> Unit = {},
    row: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    var items by remember { mutableStateOf<List<T>?>(null) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { items = runCatching { load() }.getOrDefault(emptyList()) }
    val loaded = items
    if (loaded.isNullOrEmpty()) {
        CanvasModalSheet(title, null, onClose) {
            JarnText(
                text = stringResource(
                    if (loaded == null) R.string.jarnfe_canvas_pick_loading else R.string.jarnfe_canvas_pick_empty,
                ),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textMid,
            )
        }
        return
    }
    CanvasModalSheet(title, first, onClose) {
        CanvasPickerColumn {
            loaded.forEachIndexed { index, item ->
                row(item, if (index == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
        footer()
    }
}

@Composable
internal fun CanvasAppPickerSheet(
    data: JarnCanvasData,
    onPicked: (JarnCanvasApp) -> Unit,
    onClose: () -> Unit,
) {
    CanvasLoadedPicker(
        title = stringResource(R.string.jarnfe_canvas_pick_app_title),
        load = data::installedApps,
        onClose = onClose,
    ) { app, modifier ->
        JarnNavigateRow(app.label, onClick = { onPicked(app) }, modifier = modifier.fillMaxWidth())
    }
}

@Composable
internal fun CanvasRomPickerSheet(
    data: JarnCanvasData,
    onPicked: (JarnCanvasRom) -> Unit,
    onClose: () -> Unit,
) {
    CanvasLoadedPicker(
        title = stringResource(R.string.jarnfe_canvas_pick_rom_title),
        load = data::pinnedRoms,
        onClose = onClose,
    ) { rom, modifier ->
        JarnNavigateRow(
            label = rom.title,
            supporting = rom.systemName,
            onClick = { onPicked(rom) },
            modifier = modifier.fillMaxWidth(),
        )
    }
}

/** With [rebindId], binds that same id. A cancelled prompt then leaves the id untouched. */
@Composable
internal fun CanvasWidgetPickerSheet(
    data: JarnCanvasData,
    rebindId: Int?,
    onBound: (JarnWidgetBind.Bound) -> Unit,
    onClose: () -> Unit,
) {
    var pending by remember { mutableStateOf<JarnWidgetBind.NeedsPermission?>(null) }
    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val bind = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (result.resultCode == Activity.RESULT_OK) {
            onBound(JarnWidgetBind.Bound(bind.widgetId, bind.columns, bind.rows))
        } else {
            data.cancelWidgetBind(bind)
        }
    }
    CanvasLoadedPicker(
        title = stringResource(R.string.jarnfe_canvas_pick_widget_title),
        load = data::widgetProviders,
        onClose = onClose,
    ) { provider, modifier ->
        JarnNavigateRow(
            label = provider.label,
            supporting = provider.appName,
            onClick = {
                when (val bind = data.bindWidget(provider, rebindId)) {
                    is JarnWidgetBind.Bound -> onBound(bind)
                    is JarnWidgetBind.NeedsPermission -> {
                        pending = bind
                        val launched = runCatching { permission.launch(bind.intent) }.isSuccess
                        if (!launched) {
                            pending = null
                            data.cancelWidgetBind(bind)
                            JarnNotices.show(R.string.jarnfe_canvas_notice_widget_failed)
                        }
                    }
                    JarnWidgetBind.Failed -> JarnNotices.show(R.string.jarnfe_canvas_notice_widget_failed)
                }
            },
            modifier = modifier.fillMaxWidth(),
        )
    }
}
