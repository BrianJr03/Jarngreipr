package jr.brian.frontend.ui.search

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * In-window QWERTY keyboard rendered inside the frontend's own
 * window. Fixes the Phase 1 problem #28 (bottom-screen keyboard
 * from a separate activity breaks when the top window has hidden
 * system bars).
 *
 * Rows chosen for 10-foot layout: alphabet in three rows, then
 * numeric + punctuation + control row. Space / backspace / clear
 * are explicit control keys so a gamepad user can reach them with
 * one row of D-pad movement.
 */
@Composable
fun OnScreenKeyboard(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KeyRow(keys = "1234567890".toList(), onChar = onChar)
        KeyRow(keys = "QWERTYUIOP".toList(), onChar = onChar)
        KeyRow(keys = "ASDFGHJKL".toList(), onChar = onChar)
        KeyRow(keys = "ZXCVBNM".toList(), onChar = onChar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ControlKey("space", modifier = Modifier.weight(2f)) { onChar(' ') }
            ControlKey("backspace", modifier = Modifier.weight(1f)) { onBackspace() }
            ControlKey("clear", modifier = Modifier.weight(1f)) { onClear() }
        }
    }
}

@Composable
private fun KeyRow(keys: List<Char>, onChar: (Char) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.forEach { c ->
            Key(
                label = c.toString(),
                modifier = Modifier.weight(1f),
                onClick = { onChar(c) },
            )
        }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.aspectRatio(1f).focusable(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ControlKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.focusable(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
