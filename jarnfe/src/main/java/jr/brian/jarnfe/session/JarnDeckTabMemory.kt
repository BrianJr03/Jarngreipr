package jr.brian.jarnfe.session

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

private const val PREFS_NAME = "jarnfe_session_prefs"
private const val KEY_DECK_TAB = "deck_tab"

internal fun deckTabFromName(name: String?): DeckTab? = DeckTab.entries.firstOrNull { it.name == name }

internal fun Context.rememberedDeckTab(): DeckTab? =
    deckTabFromName(getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_DECK_TAB, null))

/** Keeps the Deck on the tab the user left it on, across restarts. */
@Composable
internal fun RememberDeckTab(tab: DeckTab) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(tab) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_DECK_TAB, tab.name) }
    }
}
