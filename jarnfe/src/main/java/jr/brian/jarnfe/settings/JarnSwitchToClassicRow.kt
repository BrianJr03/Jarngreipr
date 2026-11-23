package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.findActivity
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.shell.jarnExperienceSwitchOrNull
import jr.brian.jarnfe.shell.leaveForClassic

@Composable
internal fun JarnSwitchToClassicRow() {
    val context = LocalContext.current
    val switch = remember(context) { context.jarnExperienceSwitchOrNull() } ?: return
    val activity = remember(context) { context.findActivity() } ?: return
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_switch_classic_title),
        onClick = {
            if (!leaveForClassic(activity, switch)) {
                JarnNotices.show(R.string.jarnfe_switch_classic_failed)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_switch_classic_hint),
    )
}
