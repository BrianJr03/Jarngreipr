package jr.brian.jarnfe.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSheet
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

internal const val COMPANION_PICKER_TAG = "jarn_companion_picker"

internal enum class CompanionPickerPlace { DeckWindow, DeckPanel }

/** One screen has no Deck window, so the offer rides in the Deck panel there. */
internal fun companionPickerPlace(deckDisplayAvailable: Boolean): CompanionPickerPlace =
    if (deckDisplayAvailable) CompanionPickerPlace.DeckWindow else CompanionPickerPlace.DeckPanel

/** The controller of the open companion offer, or null when nothing is offered. */
internal fun offeredCompanionController(): JarnGameActionsController? {
    val state = JarnGameActions
    val controller = state.controller
    return controller.takeIf { companionPickerShown(state.page, state.companionChoices, it != null) }
}

/** The Deck window's sheet, over whatever the Deck shows. */
@Composable
internal fun JarnCompanionSheetHost(deckDisplayAvailable: Boolean) {
    if (companionPickerPlace(deckDisplayAvailable) != CompanionPickerPlace.DeckWindow) return
    val controller = offeredCompanionController() ?: return
    val first = remember { FocusRequester() }
    CompanionPickerLayer(controller) {
        JarnSheet(
            visible = true,
            onDismiss = controller::dismissCompanions,
            modifier = Modifier.testTag(COMPANION_PICKER_TAG),
            title = stringResource(R.string.jarnfe_game_open_companion),
            initialFocus = first,
        ) { CompanionPickerRows(controller, first) }
    }
}

/** The Deck panel's body while a companion is offered. Its sheet supplies the title. */
@Composable
internal fun JarnCompanionPanelContent(controller: JarnGameActionsController) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    CompanionPickerLayer(controller) { CompanionPickerRows(controller, first, Modifier.testTag(COMPANION_PICKER_TAG)) }
}

@Composable
private fun CompanionPickerLayer(controller: JarnGameActionsController, content: @Composable () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
            on(JarnAction.Back, R.string.jarnfe_hint_back) { controller.dismissCompanions() }
        },
        content = content,
    )
}

@Composable
private fun CompanionPickerRows(
    controller: JarnGameActionsController,
    firstFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val rows = JarnGameActions.companionChoices.map { app ->
        PickerRow(app.packageName, app.label) { controller.openCompanion(app.packageName) }
    }
    PickerRows(rows, firstFocus, modifier)
}
