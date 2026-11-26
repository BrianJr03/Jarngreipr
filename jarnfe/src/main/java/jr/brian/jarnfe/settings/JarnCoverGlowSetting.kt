package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnCoverGlow
import jr.brian.jarnfe.R

/** A cycles Off and Full screen, as other Jarn value rows do. */
@Composable
internal fun JarnCoverGlowRow(choice: JarnCoverGlow, onClick: () -> Unit) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_cover_glow),
        value = stringResource(choice.labelRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_cover_glow_hint),
    )
}

@get:StringRes
private val JarnCoverGlow.labelRes: Int
    get() = when (this) {
        JarnCoverGlow.Off -> R.string.jarnfe_cover_glow_off
        JarnCoverGlow.Full -> R.string.jarnfe_cover_glow_full
    }

internal fun JarnCoverGlow.next(): JarnCoverGlow = JarnCoverGlow.entries[(ordinal + 1) % JarnCoverGlow.entries.size]
