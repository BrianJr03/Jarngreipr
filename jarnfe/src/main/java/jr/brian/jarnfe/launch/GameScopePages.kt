package jr.brian.jarnfe.launch

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.art.ArtScreen
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame

@StringRes
internal fun artScreenLabel(screen: ArtScreen): Int = when (screen) {
    ArtScreen.Top -> R.string.jarnfe_game_art_top
    ArtScreen.Bottom -> R.string.jarnfe_game_art_bottom
}

@StringRes
internal fun artChoiceLabel(choice: ArtChoice): Int = when (choice) {
    ArtChoice.Cover -> R.string.jarnfe_game_art_cover
    ArtChoice.Miximage -> R.string.jarnfe_game_art_mixed
    ArtChoice.Marquee -> R.string.jarnfe_game_art_marquee
    ArtChoice.PhysicalMedia -> R.string.jarnfe_game_art_physical
    ArtChoice.Fanart -> R.string.jarnfe_game_art_fanart
    ArtChoice.Screenshot -> R.string.jarnfe_game_art_screenshot
    ArtChoice.Titlescreen -> R.string.jarnfe_game_art_titlescreen
    ArtChoice.Default -> R.string.jarnfe_game_art_default
}

@Composable
internal fun GameScopePage(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
) {
    when (JarnGameActions.page) {
        GameActionsPage.EmulatorScope -> ScopeList(
            rows = scopeRows(
                game = game,
                onGame = { controller.applyEmulatorToGame(game) },
                onSystem = { controller.requestEmulatorForSystem(game) },
            ),
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.EmulatorConfirm -> ConfirmList(
            supporting = pluralStringResource(
                R.plurals.jarnfe_game_replace_emulators,
                JarnGameActions.replaceCount,
                JarnGameActions.replaceCount,
            ),
            onConfirm = { controller.applyEmulatorToSystem(game) },
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.Edit -> PickerList(
            titleRes = R.string.jarnfe_game_edit,
            rows = editRows(game, controller),
            emptyRes = R.string.jarnfe_game_no_apps,
            loading = false,
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.EditArt -> PickerList(
            titleRes = artScreenLabel(JarnGameActions.artScreen),
            rows = artRows(game, controller),
            emptyRes = R.string.jarnfe_game_no_apps,
            loading = JarnGameActions.artKinds == null,
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.ArtScope -> ScopeList(
            rows = scopeRows(
                game = game,
                gameEnabled = JarnGameActions.pendingArt
                    ?.let { isArtAvailable(it, JarnGameActions.artKinds.orEmpty()) } != false,
                onGame = { controller.applyArtToGame(game) },
                onSystem = { controller.requestArtForSystem(game) },
            ),
            controller = controller,
            firstFocus = firstFocus,
        )
        GameActionsPage.ArtConfirm -> ConfirmList(
            supporting = pluralStringResource(
                R.plurals.jarnfe_game_replace_art,
                JarnGameActions.replaceCount,
                JarnGameActions.replaceCount,
            ),
            onConfirm = { controller.applyArtToSystem(game) },
            controller = controller,
            firstFocus = firstFocus,
        )
        else -> Unit
    }
}

@Composable
private fun ScopeList(
    rows: List<PickerRow>,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
) {
    PickerList(
        titleRes = R.string.jarnfe_game_scope_title,
        rows = rows,
        emptyRes = R.string.jarnfe_game_no_apps,
        loading = false,
        controller = controller,
        firstFocus = firstFocus,
    )
}

@Composable
private fun ConfirmList(
    supporting: String,
    onConfirm: () -> Unit,
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
) {
    PickerList(
        titleRes = R.string.jarnfe_game_scope_confirm_title,
        rows = listOf(
            PickerRow("cancel", stringResource(R.string.jarnfe_game_scope_cancel)) { controller.back() },
            PickerRow("replace", stringResource(R.string.jarnfe_game_scope_replace), onClick = onConfirm),
        ),
        emptyRes = R.string.jarnfe_game_no_apps,
        loading = false,
        controller = controller,
        firstFocus = firstFocus,
        supporting = supporting,
    )
}

@Composable
private fun scopeRows(
    game: JarnLibraryGame,
    onGame: () -> Unit,
    onSystem: () -> Unit,
    gameEnabled: Boolean = true,
): List<PickerRow> {
    val systemTitle = JarnLibrary.state.systems.items
        .firstOrNull { it.id == game.systemId }?.title ?: game.systemId
    return listOf(
        PickerRow(
            key = "this_game",
            label = stringResource(R.string.jarnfe_game_scope_this),
            supporting = if (gameEnabled) null else stringResource(R.string.jarnfe_game_art_missing),
            enabled = gameEnabled,
            onClick = onGame,
        ),
        PickerRow(
            key = "all_games",
            label = stringResource(R.string.jarnfe_game_scope_all, systemTitle),
            onClick = onSystem,
        ),
    )
}

@Composable
private fun editRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
): List<PickerRow> {
    val choices by controller.artChoices.collectAsState()
    return ArtScreen.entries.map { screen ->
        PickerRow(
            key = screen.name,
            label = stringResource(artScreenLabel(screen)),
            supporting = stringResource(artChoiceLabel(choices.effective(screen, game.id, game.systemId))),
        ) { controller.chooseArtScreen(screen) }
    }
}

@Composable
private fun artRows(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
): List<PickerRow> {
    val choices by controller.artChoices.collectAsState()
    val screen = JarnGameActions.artScreen
    val selected = choices.effective(screen, game.id, game.systemId)
    val kinds = JarnGameActions.artKinds.orEmpty()
    val current = stringResource(R.string.jarnfe_game_current)
    val missing = stringResource(R.string.jarnfe_game_art_missing)
    return ArtChoice.entries.map { choice ->
        val notes = listOfNotNull(
            current.takeIf { choice == selected },
            missing.takeUnless { isArtAvailable(choice, kinds) },
        )
        PickerRow(
            key = choice.name,
            label = stringResource(artChoiceLabel(choice)),
            supporting = notes.joinToString(", ").ifEmpty { null },
        ) { controller.chooseArt(choice) }
    }
}
