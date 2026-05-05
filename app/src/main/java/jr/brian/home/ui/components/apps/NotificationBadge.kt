package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.managers.LocalNotificationManager

/**
 * A badge that shows the notification count for an app.
 * Should be used within a Box composable that contains the app icon.
 * 
 * @param packageName The package name of the app to show notification count for
 * @param modifier Modifier for the badge container
 * @param badgeColor Background color of the badge
 * @param textColor Color of the count text
 * @param offsetX Horizontal offset from the top-right corner
 * @param offsetY Vertical offset from the top-right corner
 */
@Composable
fun BoxScope.NotificationBadge(
    packageName: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = ThemeAccentColor,
    textColor: Color = Color.White,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    val notificationCountManager = LocalNotificationManager.current
    val notificationCounts by notificationCountManager.notificationCounts.collectAsStateWithLifecycle()
    val count = notificationCounts[packageName] ?: 0

    if (count > 0 && notificationCountManager.badgesVisible) {
        NotificationBadgeContent(
            count = count,
            modifier = modifier
                .align(Alignment.TopEnd)
                .offset(x = offsetX, y = offsetY),
            badgeColor = badgeColor,
            textColor = textColor
        )
    }
}

/**
 * Standalone notification badge that displays a count.
 * Use this when you need more control over positioning.
 * 
 * @param count The notification count to display
 * @param modifier Modifier for the badge
 * @param badgeColor Background color of the badge
 * @param textColor Color of the count text
 */
@Composable
fun NotificationBadgeContent(
    count: Int,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFFE53935),
    textColor: Color = Color.White
) {
    if (count <= 0) return
    
    val displayText = if (count > 99) "99+" else count.toString()
    val isLargeNumber = count > 9
    
    Box(
        modifier = modifier
            .then(
                if (isLargeNumber) {
                    Modifier.sizeIn(minWidth = 18.dp, minHeight = 16.dp)
                } else {
                    Modifier.size(16.dp)
                }
            )
            .clip(CircleShape)
            .background(badgeColor)
            .padding(horizontal = if (isLargeNumber) 3.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 9.sp,
            maxLines = 1
        )
    }
}

/**
 * A wrapper composable that adds a notification badge to any content.
 * 
 * @param packageName The package name of the app to show notification count for
 * @param modifier Modifier for the container
 * @param badgeOffsetX Horizontal offset for the badge from the top-right corner
 * @param badgeOffsetY Vertical offset for the badge from the top-right corner
 * @param content The content to wrap (typically an app icon)
 */
@Composable
fun WithNotificationBadge(
    packageName: String,
    modifier: Modifier = Modifier,
    badgeOffsetX: Dp = 4.dp,
    badgeOffsetY: Dp = (-4).dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        NotificationBadge(
            packageName = packageName,
            offsetX = badgeOffsetX,
            offsetY = badgeOffsetY
        )
    }
}
