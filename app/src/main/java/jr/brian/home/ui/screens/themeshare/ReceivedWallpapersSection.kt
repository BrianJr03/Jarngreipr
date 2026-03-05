package jr.brian.home.ui.screens.themeshare

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import kotlinx.coroutines.launch

@Composable
fun ReceivedWallpapersSection(
    receivedWallpapers: Map<String, String>,
    onDelete: (String) -> Unit
) {
    if (receivedWallpapers.isEmpty()) return

    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val scope = rememberCoroutineScope()

    Text(
        text = stringResource(R.string.nearby_wallpaper_received_title),
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
    )

    receivedWallpapers.forEach { (key, uriString) ->
        val mediaType = when {
            uriString.endsWith(".gif") -> WallpaperType.GIF
            uriString.endsWith(".mp4") || uriString.endsWith(".webm") -> WallpaperType.VIDEO
            else -> WallpaperType.IMAGE
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReceivedMediaThumbnail(
                uriString = uriString,
                type = mediaType,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(ThemeSecondaryColor.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                        .border(1.dp, ThemeSecondaryColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                        .clickable {
                            wallpaperManager.setWallpaper(uriString, mediaType)
                            when (mediaType) {
                                WallpaperType.GIF -> wallpaperManager.updateSavedGifUri(uriString)
                                WallpaperType.VIDEO -> wallpaperManager.updateSavedVideoUri(uriString)
                                else -> wallpaperManager.updateSavedImageUri(uriString)
                            }
                            Toast.makeText(context, "Applied Wallpaper", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = ThemeSecondaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.nearby_wallpaper_apply),
                        color = ThemeSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                saveWallpaperToDownloads(context, uriString, mediaType)
                            }
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to device",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFE53935).copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    .clickable(
                        onClick = { onDelete(key) },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.nearby_wallpaper_delete_description),
                    tint = Color(0xFFE53935).copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
