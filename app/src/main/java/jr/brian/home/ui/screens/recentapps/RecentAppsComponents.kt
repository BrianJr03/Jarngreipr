package jr.brian.home.ui.screens.recentapps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.RecentAppsUtil

/**
 * Reusable button for display preference selection (Top/Bottom)
 */
@Composable
fun DisplayButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor =
        if (isSelected) selectedColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    val contentColor = if (isSelected) selectedColor else Color.White.copy(alpha = 0.7f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

/**
 * Container for app icon with themed background
 */
@Composable
fun AppIconContainer(
    icon: Drawable,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(88.dp),
        shape = RoundedCornerShape(22.dp),
        color = ThemePrimaryColor.copy(alpha = 0.12f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = rememberAsyncImagePainter(model = icon),
                contentDescription = stringResource(
                    R.string.recent_apps_icon_description,
                    label
                ),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * Badge displaying app usage time
 */
@Composable
fun UsageTimeBadge(
    usageTimeMs: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ThemeSecondaryColor.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = ThemeSecondaryColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = RecentAppsUtil.formatUsageDuration(usageTimeMs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
