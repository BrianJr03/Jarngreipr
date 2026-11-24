package jr.brian.jarnfe.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.firstrun.JarnReadyCard
import jr.brian.jarnfe.firstrun.JarnReadyRow
import jr.brian.jarnfe.firstrun.JarnReadyRowSource
import jr.brian.jarnfe.firstrun.JarnReadyRows

private const val HOME_ROW_KEY = "home"
private const val HOME_ROW_ORDER = 50

/** The Ready card's last line: green once Jarngreipr is Android's Home app, a button until then. */
internal object JarnHomeReadyRowSource : JarnReadyRowSource {
    @Composable
    override fun row(card: JarnReadyCard): JarnReadyRow? {
        val home = rememberJarnHomeAppState() ?: return null
        return jarnHomeReadyRow(
            isHome = home.isHome,
            doneText = stringResource(R.string.jarnfe_home_ready_done),
            todoText = stringResource(R.string.jarnfe_home_ready_todo),
            actionLabel = stringResource(R.string.jarnfe_home_ready_action),
            onAction = home::openPicker,
        )
    }

    fun register() = JarnReadyRows.register(HOME_ROW_KEY, this)
}

internal fun jarnHomeReadyRow(
    isHome: Boolean,
    doneText: String,
    todoText: String,
    actionLabel: String,
    onAction: () -> Unit,
): JarnReadyRow = if (isHome) {
    JarnReadyRow(key = HOME_ROW_KEY, text = doneText, isDone = true, order = HOME_ROW_ORDER)
} else {
    JarnReadyRow(
        key = HOME_ROW_KEY,
        text = todoText,
        isDone = false,
        actionLabel = actionLabel,
        onAction = onAction,
        order = HOME_ROW_ORDER,
    )
}
