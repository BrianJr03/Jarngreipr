package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.themes.JarnThemeShareCheck
import jr.brian.jarnfe.themes.JarnThemeShareResult
import jr.brian.jarnfe.themes.canShare
import jr.brian.jarnfe.themes.shareTheme
import kotlinx.coroutines.launch

/**
 * Share as a `.jarntheme` file. A layout that can't be shared shows why and stays focusable, so
 * the reason can be read. [beforeShare] runs first, as when the editor saves the draft, and
 * [onDone] once the sheet is up, so a menu holding this row stays open until then.
 */
@Composable
internal fun JarnThemeShareRow(themeId: String, onDone: () -> Unit = {}, beforeShare: suspend () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val check = canShare(themeId)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_editor_share),
        onClick = {
            scope.launch {
                beforeShare()
                val result = context.shareTheme(themeId)
                if (result is JarnThemeShareResult.NotShared) JarnNotices.show(result.messageRes)
                onDone()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        supporting = (check as? JarnThemeShareCheck.NotShareable)?.let { stringResource(it.messageRes) },
        enabled = check is JarnThemeShareCheck.Shareable,
        focusableWhenDisabled = true,
    )
}
