package jr.brian.home.ui.screens.themeshare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun ThemeSharingSection(
    isPinging: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onPingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsSection(
        title = stringResource(R.string.theme_sharing_subtitle),
        icon = Icons.Default.ColorLens,
        isExpanded = isExpanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isPinging) ThemeSecondaryColor.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinging) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isPinging) ThemeSecondaryColor else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isPinging) "Broadcasting your theme…" else "Not sharing",
                color = if (isPinging) ThemeSecondaryColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
                letterSpacing = 0.1.sp
            )
        }

        Button(
            onClick = onPingClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPinging)
                    Color(0xFFE53935).copy(alpha = 0.12f)
                else
                    ThemeSecondaryColor.copy(alpha = 0.18f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isPinging) Color(0xFFE53935).copy(alpha = 0.35f)
                else ThemeSecondaryColor.copy(alpha = 0.35f)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(
                imageVector = if (isPinging) Icons.Default.Stop else Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (isPinging) Color(0xFFE53935) else ThemeSecondaryColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPinging) stringResource(R.string.theme_sharing_stop)
                else stringResource(R.string.theme_sharing_ping),
                color = if (isPinging) Color(0xFFE53935) else ThemeSecondaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}
