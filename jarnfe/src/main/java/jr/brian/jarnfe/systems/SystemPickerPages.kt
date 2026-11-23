package jr.brian.jarnfe.systems

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private data class SystemPickerRow(
    val key: String,
    val label: String,
    val supporting: String? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun SystemEmulatorPage(
    system: JarnManagedSystem,
    allApps: Boolean,
    manager: JarnSystemManager,
    onBack: () -> Unit,
    onChooseAnother: () -> Unit,
) {
    val choose = { packageName: String ->
        manager.setEmulator(system.id, packageName)
        onBack()
    }
    val rows = if (allApps) {
        installedAppRows(manager, system, choose)
    } else {
        compatibleEmulatorRows(manager, system, choose, onChooseAnother)
    }
    SystemPickerPage(
        title = stringResource(
            if (allApps) R.string.jarnfe_game_choose_app else R.string.jarnfe_game_choose_emulator,
        ),
        rows = rows,
        focusKey = allApps,
        emptyRes = R.string.jarnfe_systems_emulator_no_games,
        onBack = onBack,
    )
}

@Composable
private fun compatibleEmulatorRows(
    manager: JarnSystemManager,
    system: JarnManagedSystem,
    choose: (String) -> Unit,
    onChooseAnother: () -> Unit,
): List<SystemPickerRow>? {
    val options by produceState<EmulatorLoad>(EmulatorLoad.Loading, system.id) {
        value = EmulatorLoad.Loaded(manager.emulators(system.id))
    }
    val another = stringResource(R.string.jarnfe_game_choose_app)
    return when (val load = options) {
        EmulatorLoad.Loading -> null
        is EmulatorLoad.Loaded -> load.options?.let { list ->
            list.map { option ->
                SystemPickerRow("${option.packageName}|${option.command}", option.label) {
                    choose(option.packageName)
                }
            } + SystemPickerRow("choose_another_app", another, onClick = onChooseAnother)
        }.orEmpty()
    }
}

private sealed interface EmulatorLoad {
    data object Loading : EmulatorLoad
    data class Loaded(val options: List<jr.brian.jarnfe.launch.JarnEmulatorOption>?) : EmulatorLoad
}

@Composable
private fun installedAppRows(
    manager: JarnSystemManager,
    system: JarnManagedSystem,
    choose: (String) -> Unit,
): List<SystemPickerRow>? {
    val apps by produceState<List<jr.brian.jarnfe.launch.JarnAppOption>?>(null, system.id) {
        value = manager.installedApps()
    }
    return apps?.map { app -> SystemPickerRow(app.packageName, app.label) { choose(app.packageName) } }
}

@Composable
private fun SystemPickerPage(
    title: String,
    rows: List<SystemPickerRow>?,
    focusKey: Any,
    emptyRes: Int,
    onBack: () -> Unit,
    supporting: String? = null,
) {
    val firstFocus = remember { FocusRequester() }
    SystemsPageFocus(firstFocus, focusKey to (rows != null))
    SystemsPageColumn {
        JarnText(title, JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 2)
        supporting?.let { JarnText(it, JarnTheme.type.body, JarnTheme.colors.textMid, maxLines = 2) }
        when {
            rows == null -> SystemsMessage(R.string.jarnfe_game_loading)
            rows.isEmpty() -> {
                SystemsMessage(emptyRes)
                JarnButton(
                    label = stringResource(R.string.jarnfe_hint_back),
                    onClick = onBack,
                    modifier = Modifier.focusRequester(firstFocus),
                    variant = JarnButtonVariant.Secondary,
                )
            }
            else -> rows.forEachIndexed { index, row ->
                JarnNavigateRow(
                    label = row.label,
                    supporting = row.supporting,
                    onClick = row.onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier),
                )
            }
        }
    }
}

@Composable
internal fun AddSystemChoosePage(
    treeUri: Uri,
    manager: JarnSystemManager,
    onNoGames: (String) -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val choices by produceState<List<JarnSystemChoice>?>(null, treeUri) {
        value = manager.systemChoices(treeUri)
    }
    val suggested = stringResource(R.string.jarnfe_systems_add_suggested)
    val rows = choices?.map { choice ->
        SystemPickerRow(choice.id, choice.title, suggested.takeIf { choice.matchesFolder }) {
            if (busy) return@SystemPickerRow
            busy = true
            scope.launch {
                when (manager.checkFolder(choice.id, treeUri)) {
                    is JarnFolderCheck.Found -> finishAdd(manager, choice.id, treeUri, onDone)
                    JarnFolderCheck.NoGames -> onNoGames(choice.id)
                    JarnFolderCheck.AccessLost -> {
                        JarnNotices.show(R.string.jarnfe_systems_folder_access_lost)
                        onDone()
                    }
                }
                busy = false
            }
        }
    }
    SystemPickerPage(
        title = stringResource(R.string.jarnfe_systems_add_pick_title),
        rows = rows,
        focusKey = treeUri,
        emptyRes = R.string.jarnfe_systems_add_no_choices,
        onBack = onDone,
        supporting = stringResource(R.string.jarnfe_systems_add_pick_hint),
    )
}

private suspend fun finishAdd(
    manager: JarnSystemManager,
    systemId: String,
    treeUri: Uri,
    onDone: () -> Unit,
) {
    val saved = manager.addFolder(systemId, treeUri)
    JarnNotices.show(if (saved) R.string.jarnfe_systems_add_done else R.string.jarnfe_systems_folder_failed)
    onDone()
}

@Composable
internal fun AddSystemNoGamesPage(
    treeUri: Uri,
    systemId: String,
    manager: JarnSystemManager,
    onChooseAnother: () -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val firstFocus = remember { FocusRequester() }
    SystemsPageFocus(firstFocus, systemId)
    SystemsPageColumn {
        JarnText(
            text = stringResource(R.string.jarnfe_systems_folder_no_games),
            style = JarnTheme.type.heading,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_systems_add_choose_another),
            onClick = onChooseAnother,
            modifier = Modifier.fillMaxWidth().focusRequester(firstFocus),
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_systems_add_keep),
            onClick = { scope.launch { finishAdd(manager, systemId, treeUri, onDone) } },
            modifier = Modifier.fillMaxWidth(),
            variant = JarnButtonVariant.Secondary,
        )
    }
}
