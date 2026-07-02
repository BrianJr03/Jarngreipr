package jr.brian.home.ui.screens.themeshare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalThemeManager

@Composable
fun NearbyWallpaperSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeManager = LocalThemeManager.current
    val discoveredEndpoints by themeManager.nearbyDiscoveredEndpoints.collectAsStateWithLifecycle()
    val connectedEndpoints by themeManager.connectedEndpoints.collectAsStateWithLifecycle()
    val transferProgress by themeManager.transferProgress.collectAsStateWithLifecycle()
    val failedTransferEndpoints by themeManager.failedTransferEndpoints.collectAsStateWithLifecycle()
    val isDiscoveringWallpaper = themeManager.isWallpaperNearbyRunning

    CollapsibleSettingsSection(
        title = stringResource(R.string.nearby_wallpaper_subtitle),
        icon = Icons.Default.Wallpaper,
        isExpanded = isExpanded,
        onToggle = onToggle,
        modifier = modifier
    ) {
        // Status row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isDiscoveringWallpaper)
                            Color(0xFF4CAF50).copy(alpha = 0.18f)
                        else
                            Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDiscoveringWallpaper) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isDiscoveringWallpaper) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                val currentUri = themeManager.getCurrentWallpaperUri()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = if (currentUri != null) Color(0xFF4CAF50) else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    if (currentUri != null) {
                        val isEsde = themeManager.isEsdeModeActive()
                        Text(
                            text = if (isEsde)
                                stringResource(R.string.nearby_wallpaper_current_esde)
                            else
                                stringResource(R.string.nearby_wallpaper_current_path),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.nearby_wallpaper_no_wallpaper),
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Start/Stop button
        Button(
            onClick = {
                if (isDiscoveringWallpaper) themeManager.stopWallpaperNearby()
                else themeManager.startWallpaperNearby()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDiscoveringWallpaper)
                    Color(0xFFE53935).copy(alpha = 0.12f)
                else
                    Color(0xFF4CAF50).copy(alpha = 0.12f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDiscoveringWallpaper) Color(0xFFE53935).copy(alpha = 0.35f)
                else Color(0xFF4CAF50).copy(alpha = 0.35f)
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(
                imageVector = if (isDiscoveringWallpaper) Icons.Default.Stop else Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (isDiscoveringWallpaper) Color(0xFFE53935) else Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDiscoveringWallpaper)
                    stringResource(R.string.nearby_wallpaper_stop)
                else
                    stringResource(R.string.nearby_wallpaper_start),
                color = if (isDiscoveringWallpaper) Color(0xFFE53935) else Color(0xFF4CAF50),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }

        // Discovered endpoints
        if (isDiscoveringWallpaper) {
            if (discoveredEndpoints.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Text(
                        text = stringResource(R.string.nearby_wallpaper_scanning),
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
            } else {
                discoveredEndpoints.forEach { (endpointId, deviceName) ->
                    val isConnected = endpointId in connectedEndpoints
                    val transferState = transferProgress[endpointId]
                    val progress = transferState?.fraction
                    val hasFailed = endpointId in failedTransferEndpoints

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isConnected)
                                    Color(0xFF4CAF50).copy(alpha = 0.06f)
                                else
                                    Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (isConnected)
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else
                                    Color.White.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (isConnected) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                                Column {
                                    Text(
                                        text = deviceName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isConnected) "Connected" else "Discovered",
                                        color = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            val hasWallpaper = themeManager.getCurrentWallpaperUri() != null
                            val canSend = hasWallpaper && isConnected && (transferState == null || transferState.isComplete)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (canSend) ThemeSecondaryColor.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (canSend) ThemeSecondaryColor.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = canSend) {
                                        themeManager.sendWallpaperTo(endpointId)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.nearby_wallpaper_send),
                                    color = if (canSend) ThemeSecondaryColor else Color.Gray.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }

                        AnimatedVisibility(visible = progress != null || hasFailed) {
                            if (hasFailed) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935).copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.nearby_wallpaper_transfer_failed),
                                        color = Color(0xFFE53935).copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Sending…",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "${((progress ?: 0f) * 100).toInt()}%",
                                            color = ThemeSecondaryColor.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress ?: 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = ThemeSecondaryColor,
                                        trackColor = Color.White.copy(alpha = 0.08f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
