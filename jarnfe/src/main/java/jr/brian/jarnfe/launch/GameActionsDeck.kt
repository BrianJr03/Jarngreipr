package jr.brian.jarnfe.launch

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.wallpaper.jarnDisabledRowBacking

internal data class PickerRow(
    val key: String,
    val label: String,
    val supporting: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal fun GameActionsDeck(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    details: @Composable (actions: @Composable () -> Unit) -> Unit,
) {
    GameActionsHost(game, controller) { playFocus ->
        details { GameActionRows(game, controller, playFocus) }
    }
}

/** Runs the action pages and lets [actionsPage] place Play, which must use the given requester. */
@Composable
internal fun GameActionsHost(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    actionsPage: @Composable (playFocus: FocusRequester) -> Unit,
) {
    val state = JarnGameActions
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(game.id, controller) { controller.show(game) }
    PendingFocusEffect(firstFocus, pageIsReady(state.page))
    JarnInputLayer(
        enabled = state.page != GameActionsPage.Actions,
        bindings = { on(JarnAction.Back, R.string.jarnfe_hint_back) { controller.back() } },
    ) {
        if (state.page == GameActionsPage.Actions) {
            actionsPage(firstFocus)
        } else {
            GameActionPicker(game, controller, firstFocus)
        }
    }
}

private fun pageIsReady(page: GameActionsPage): Boolean = when (page) {
    GameActionsPage.Actions -> true
    GameActionsPage.Emulators -> JarnGameActions.setup != null
    GameActionsPage.Cores -> !coresLoading()
    GameActionsPage.EditArt, GameActionsPage.ArtScope -> JarnGameActions.artKinds != null
    GameActionsPage.EmulatorApps, GameActionsPage.Companions -> JarnGameActions.apps != null
    GameActionsPage.CompanionLaunch,
    GameActionsPage.EmulatorScope,
    GameActionsPage.EmulatorConfirm,
    GameActionsPage.Edit,
    GameActionsPage.ArtConfirm,
    -> true
}

private fun coresLoading(): Boolean = if (JarnGameActions.pendingEmulator != null) {
    JarnGameActions.pendingCores == null
} else {
    JarnGameActions.setup == null
}

@Composable
private fun PendingFocusEffect(requester: FocusRequester, ready: Boolean) {
    val state = JarnGameActions
    LaunchedEffect(state.pendingFocus, state.page, ready) {
        if (!state.pendingFocus || !ready) return@LaunchedEffect
        withFrameNanos { }
        runCatching { requester.requestFocus() }
        state.focusHandled()
    }
}

@Composable
private fun GameActionRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    playFocus: FocusRequester,
) {
    val setup = JarnGameActions.setup
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        JarnButton(
            label = stringResource(R.string.jarnfe_hint_play),
            onClick = { JarnLibrary.play(game) },
            modifier = Modifier.fillMaxWidth().focusRequester(playFocus),
        )
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_game_favorite),
            checked = setup?.isFavorite == true,
            onCheckedChange = { controller.toggleFavorite(game) },
            enabled = setup != null,
        )
        GameSetupRows(game, controller)
    }
}

/** Every row after Play and Favorite. Place inside a Column. */
@Composable
internal fun GameSetupRows(game: JarnLibraryGame, controller: JarnGameActionsController) {
    val setup = JarnGameActions.setup
    JarnValueRow(
        label = stringResource(R.string.jarnfe_game_companion),
        value = setup?.companions?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.label }
            ?: stringResource(R.string.jarnfe_game_none),
        onClick = { controller.openPage(GameActionsPage.Companions) },
    )
    JarnValueRow(
        label = stringResource(R.string.jarnfe_game_emulator),
        value = setup?.currentEmulatorLabel ?: stringResource(R.string.jarnfe_game_not_set),
        onClick = { controller.openPage(GameActionsPage.Emulators) },
    )
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_game_edit),
        onClick = { controller.openEdit(game) },
    )
    if (setup?.usesRetroArch == true) CoreRow(setup, controller)
}

@Composable
private fun CoreRow(setup: JarnGameSetup, controller: JarnGameActionsController) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_game_core),
        value = setup.cores.firstOrNull { it.path == setup.currentCore }?.label
            ?: stringResource(R.string.jarnfe_game_not_set),
        onClick = { controller.openPage(GameActionsPage.Cores) },
    )
}

@Composable
private fun GameActionPicker(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
) {
    when (JarnGameActions.page) {
        GameActionsPage.Actions -> Unit
        GameActionsPage.Emulators -> PickerList(
            titleRes = R.string.jarnfe_game_choose_emulator,
            rows = emulatorRows(game, controller),
            emptyRes = R.string.jarnfe_game_no_emulators,
            loading = JarnGameActions.setup == null,
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.EmulatorApps -> PickerList(
            titleRes = R.string.jarnfe_game_choose_app,
            rows = emulatorAppRows(game, controller),
            emptyRes = R.string.jarnfe_game_no_apps,
            loading = JarnGameActions.apps == null,
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.Cores -> PickerList(
            titleRes = R.string.jarnfe_game_choose_core,
            rows = coreRows(controller),
            emptyRes = R.string.jarnfe_game_no_cores,
            loading = coresLoading(),
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.Companions -> PickerList(
            titleRes = R.string.jarnfe_game_choose_companion,
            rows = companionRows(game, controller),
            emptyRes = R.string.jarnfe_game_no_apps,
            loading = JarnGameActions.apps == null,
            controller = controller,
            firstFocus = firstFocus,
            supporting = stringResource(R.string.jarnfe_game_companion_limit),
        )
        GameActionsPage.CompanionLaunch -> PickerList(
            titleRes = R.string.jarnfe_game_open_companion,
            rows = JarnGameActions.companionChoices.map { app ->
                PickerRow(app.packageName, app.label) { controller.openCompanion(app.packageName) }
            },
            emptyRes = R.string.jarnfe_game_no_apps,
            loading = false,
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.EmulatorScope,
        GameActionsPage.EmulatorConfirm,
        GameActionsPage.Edit,
        GameActionsPage.EditArt,
        GameActionsPage.ArtScope,
        GameActionsPage.ArtConfirm,
        -> GameScopePage(game, controller, firstFocus)
    }
}

@Composable
private fun emulatorRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
): List<PickerRow> {
    val setup = JarnGameActions.setup ?: return emptyList()
    val current = stringResource(R.string.jarnfe_game_current)
    val options = setup.emulators.map { option ->
        val expected = option.expectedExtension?.let {
            stringResource(R.string.jarnfe_game_expects_extension, it)
        }
        val marker = current.takeIf { option.packageName == setup.currentEmulator }
        PickerRow(
            key = "${option.packageName}|${option.command}",
            label = option.label,
            supporting = listOfNotNull(marker, expected).joinToString(", ").ifEmpty { null },
        ) { controller.chooseEmulator(game, option) }
    }
    val another = PickerRow(
        key = "choose_another_app",
        label = stringResource(R.string.jarnfe_game_choose_app),
    ) { controller.openPage(GameActionsPage.EmulatorApps) }
    return options + another
}

@Composable
private fun emulatorAppRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
): List<PickerRow> {
    val currentPackage = JarnGameActions.setup?.currentEmulator
    val current = stringResource(R.string.jarnfe_game_current)
    return JarnGameActions.apps.orEmpty().map { app ->
        PickerRow(app.packageName, app.label, current.takeIf { app.packageName == currentPackage }) {
            controller.chooseEmulator(game, JarnEmulatorOption(app.packageName, app.label))
        }
    }
}

@Composable
private fun coreRows(controller: JarnGameActionsController): List<PickerRow> {
    val state = JarnGameActions
    val setup = state.setup
    val cores = state.pendingCores ?: setup?.cores.orEmpty()
    val current = stringResource(R.string.jarnfe_game_current)
    return cores.map { core ->
        PickerRow(core.path, core.label, current.takeIf { core.path == setup?.currentCore }) {
            controller.chooseCore(core.path)
        }
    }
}

@Composable
private fun companionRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
): List<PickerRow> {
    val selected = JarnGameActions.setup?.companions.orEmpty().map { it.packageName }.toSet()
    val current = stringResource(R.string.jarnfe_game_current)
    val apps = JarnGameActions.apps.orEmpty().map { app ->
        PickerRow(app.packageName, app.label, current.takeIf { app.packageName in selected }) {
            controller.addCompanion(game, app.packageName)
        }
    }
    if (selected.isEmpty()) return apps
    val clear = PickerRow("clear_companions", stringResource(R.string.jarnfe_game_clear_companions)) {
        controller.clearCompanions(game)
    }
    return listOf(clear) + apps
}

@Composable
internal fun PickerList(
    @StringRes titleRes: Int,
    rows: List<PickerRow>,
    @StringRes emptyRes: Int,
    loading: Boolean,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
    supporting: String? = null,
) {
    val colors = JarnTheme.colors
    JarnText(stringResource(titleRes), JarnTheme.type.heading, colors.textHigh, maxLines = 2)
    supporting?.let {
        JarnText(it, JarnTheme.type.body, colors.textMid, maxLines = 3)
    }
    when {
        loading -> JarnText(stringResource(R.string.jarnfe_game_loading), JarnTheme.type.body, colors.textMid)
        rows.isEmpty() -> PickerEmpty(emptyRes, controller, firstFocus)
        else -> PickerRows(rows, firstFocus)
    }
}

@Composable
private fun PickerRows(rows: List<PickerRow>, firstFocus: FocusRequester) {
    val focusIndex = rows.indexOfFirst { it.enabled }
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        rows.forEachIndexed { index, row ->
            JarnNavigateRow(
                label = row.label,
                supporting = row.supporting,
                onClick = row.onClick,
                enabled = row.enabled,
                modifier = Modifier
                    .jarnDisabledRowBacking(row.enabled)
                    .then(if (index == focusIndex) Modifier.focusRequester(firstFocus) else Modifier),
            )
        }
    }
}

@Composable
private fun PickerEmpty(
    @StringRes messageRes: Int,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
) {
    JarnText(stringResource(messageRes), JarnTheme.type.body, JarnTheme.colors.textMid, maxLines = 3)
    JarnButton(
        label = stringResource(R.string.jarnfe_hint_back),
        onClick = controller::back,
        modifier = Modifier.focusRequester(firstFocus),
        variant = JarnButtonVariant.Secondary,
    )
}
