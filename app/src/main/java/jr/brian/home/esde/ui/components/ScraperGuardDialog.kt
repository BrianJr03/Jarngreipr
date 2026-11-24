package jr.brian.home.esde.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import jr.brian.home.R
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor

@Composable
fun ScraperGuardDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.scraper_guard_dismiss),
                    color = ThemeAccentColor
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.scraper_guard_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.scraper_guard_message),
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        containerColor = OledBackgroundColor
    )
}
