package jr.brian.home.ui.screens.themeshare

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.managers.LocalThemeManager

@Composable
fun ReceivedThemesSection(
    receivedThemes: Map<String, List<ColorTheme>>,
    onDeleteTheme: (String, String) -> Unit
) {
    val context = LocalContext.current
    val themeManager = LocalThemeManager.current

    if (receivedThemes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.theme_sharing_empty),
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        receivedThemes.forEach { (displayName, themes) ->
            Text(
                text = displayName,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                items(themes) { theme ->
                    ReceivedThemeCard(
                        theme = theme,
                        onClick = {
                            themeManager.addCustomTheme(theme)
                            themeManager.setTheme(theme)
                            Toast.makeText(context, "Applied Theme", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { onDeleteTheme(displayName, theme.id) }
                    )
                }
            }
        }
    }
}
