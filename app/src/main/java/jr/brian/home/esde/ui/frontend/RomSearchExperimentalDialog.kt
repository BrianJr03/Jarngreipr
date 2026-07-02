package jr.brian.home.esde.ui.frontend

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import jr.brian.home.R
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor

private const val PREFS_NAME = "rom_search_prefs"
private const val KEY_EXPERIMENTAL_SHOWN = "experimental_shown"

@Composable
fun RomSearchExperimentalDialogIfNeeded() {
    val context = LocalContext.current
    var visible by remember {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        mutableStateOf(!prefs.getBoolean(KEY_EXPERIMENTAL_SHOWN, false))
    }
    if (!visible) return

    val markShown: () -> Unit = {
        visible = false
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_EXPERIMENTAL_SHOWN, true) }
    }

    AlertDialog(
        onDismissRequest = markShown,
        containerColor = OledCardColor,
        title = { DialogTitleText() },
        text = { DialogBodyText() },
        confirmButton = { DialogConfirmButton(onClick = markShown) },
        dismissButton = {}
    )
}

@Composable
private fun DialogTitleText() {
    Text(
        text = stringResource(R.string.rom_search_experimental_title),
        color = Color.White.copy(alpha = 0.9f),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DialogBodyText() {
    Text(
        text = stringResource(R.string.rom_search_experimental_message),
        color = Color.White.copy(alpha = 0.75f),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DialogConfirmButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.rom_search_experimental_got_it),
            color = ThemeAccentColor
        )
    }
}
