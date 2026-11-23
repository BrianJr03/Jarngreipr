package jr.brian.jarnfe.apps

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.canvas.CanvasSheetListMaxHeight
import jr.brian.jarnfe.canvas.JarnAppDisplay
import jr.brian.jarnfe.canvas.opposite

@StringRes
private fun JarnAppDisplay.labelRes(): Int = when (this) {
    JarnAppDisplay.Primary -> R.string.jarnfe_folder_display_top
    JarnAppDisplay.Current -> R.string.jarnfe_folder_display_bottom
}

@Composable
private fun OptionsColumn(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(scroll)
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) { content() }
    }
}

@Composable
internal fun JarnAppOptionsMenu(
    data: JarnAppOptionsData,
    target: JarnAppOptionsTarget,
    extras: List<JarnAppOptionsExtra>,
    onOpenOther: (JarnAppDisplay) -> Unit,
    onAppInfo: () -> Unit,
    onIcon: () -> Unit,
    onRename: () -> Unit,
    onListChange: () -> Unit,
    onClose: () -> Unit,
) {
    var options by remember(target.packageName, target.label) {
        mutableStateOf(data.appOptions(target.packageName))
    }
    val first = remember { FocusRequester() }
    CanvasModalSheet(target.label, first, onClose) {
        OptionsColumn {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_canvas_action_app_info),
                onClick = onAppInfo,
                modifier = Modifier.fillMaxWidth().focusRequester(first),
            )
            if (options.hasSecondDisplay) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_open_other),
                    onClick = { onOpenOther(options.display) },
                    modifier = Modifier.fillMaxWidth(),
                )
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_launch_on),
                    supporting = stringResource(options.display.labelRes()),
                    onClick = {
                        val next = options.display.opposite()
                        data.setAppDisplay(target.packageName, next)
                        options = options.copy(display = next)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                JarnToggleRow(
                    label = stringResource(R.string.jarnfe_folder_ask_display),
                    checked = options.askOnLaunch,
                    onCheckedChange = { ask ->
                        data.setAskDisplayOnLaunch(target.packageName, ask)
                        options = options.copy(askOnLaunch = ask)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_app_icon_title),
                onClick = onIcon,
                modifier = Modifier.fillMaxWidth(),
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_folder_rename_app),
                onClick = onRename,
                modifier = Modifier.fillMaxWidth(),
            )
            if (options.customName != null) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_reset_name),
                    onClick = {
                        first.requestFocus()
                        data.setAppName(target.packageName, null)
                        options = options.copy(customName = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            JarnNavigateRow(
                label = stringResource(
                    if (options.pinned) R.string.jarnfe_apps_action_unpin else R.string.jarnfe_apps_action_pin,
                ),
                onClick = {
                    val next = !options.pinned
                    onListChange()
                    data.setPinned(target.packageName, next)
                    options = options.copy(pinned = next)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            JarnNavigateRow(
                label = stringResource(
                    if (options.hidden) R.string.jarnfe_apps_action_unhide else R.string.jarnfe_apps_action_hide,
                ),
                onClick = {
                    val next = !options.hidden
                    onListChange()
                    data.setHidden(target.packageName, next)
                    options = options.copy(hidden = next)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            extras.forEach { extra ->
                JarnNavigateRow(
                    label = stringResource(extra.label),
                    onClick = extra.onClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun JarnAppIconSheet(
    data: JarnAppOptionsData,
    target: JarnAppOptionsTarget,
    onChooseImage: () -> Unit,
    onFromPack: () -> Unit,
    onAllAppsPack: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val selected by remember(data) { data.selectedIconPack }.collectAsState(null)
    val packs by produceState<List<JarnIconPack>>(emptyList(), data) { value = data.iconPacks() }
    val packName = packs.firstOrNull { it.packageName == selected }?.name
        ?: stringResource(R.string.jarnfe_app_icon_pack_none)
    CanvasModalSheet(stringResource(R.string.jarnfe_app_icon_title), first, onClose) {
        OptionsColumn {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_folder_choose_icon),
                onClick = onChooseImage,
                modifier = Modifier.fillMaxWidth().focusRequester(first),
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_app_icon_from_pack),
                onClick = onFromPack,
                modifier = Modifier.fillMaxWidth(),
            )
            if (target.hasCustomIcon) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_remove_icon),
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_app_icon_pack_all),
                supporting = packName,
                onClick = onAllAppsPack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
