package jr.brian.home.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.screens.ShortcutOptionItem

@Composable
fun BackButtonShortcutSlotSelector(
    isSecondarySelected: Boolean,
    onSlotSelected: (isSecondary: Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShortcutOptionItem(
            label = stringResource(R.string.back_button_shortcut_slot_primary),
            isSelected = !isSecondarySelected,
            onClick = { onSlotSelected(false) },
            modifier = Modifier.weight(1f)
        )
        ShortcutOptionItem(
            label = stringResource(R.string.back_button_shortcut_slot_secondary),
            isSelected = isSecondarySelected,
            onClick = { onSlotSelected(true) },
            modifier = Modifier.weight(1f)
        )
    }
}
