package jr.brian.jarnfe.firstrun

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.library.label
import jr.brian.jarnfe.library.rememberJarnGamesFolderAction

/** Replaces the built-in games folder row: ES-DE's ROMs folder for ES-DE players, else Add game folder. */
internal object JarnEsdeRomsReadyRow : JarnReadyRowSource {
    @Composable
    override fun row(card: JarnReadyCard): JarnReadyRow? {
        if (card.hasGames) return null
        val action = rememberJarnGamesFolderAction(LocalJarnScreen.current)
        return JarnReadyRow(
            key = READY_ROW_GAMES_FOLDER,
            text = stringResource(R.string.jarnfe_esde_roms_row),
            isDone = false,
            actionLabel = action.label(),
            onAction = action.run,
            linkLabel = stringResource(R.string.jarnfe_esde_roms_skip),
            onLink = card.skipToLook,
            leads = true,
        )
    }
}
