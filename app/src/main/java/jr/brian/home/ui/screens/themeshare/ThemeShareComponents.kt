package jr.brian.home.ui.screens.themeshare

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.managers.WallpaperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

suspend fun saveWallpaperToDownloads(
    context: Context,
    uriString: String,
    mediaType: WallpaperType
) {
    withContext(Dispatchers.IO) {
        try {
            val uri = uriString.toUri()
            val mimeType = context.contentResolver.getType(uri)
            val extension = when (mediaType) {
                WallpaperType.GIF -> "gif"
                WallpaperType.VIDEO -> "mp4"
                else -> "jpg"
            }
            val fileName = "wallpaper_${System.currentTimeMillis()}.$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val destinationUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (destinationUri != null) {
                resolver.openOutputStream(destinationUri).use { outputStream ->
                    resolver.openInputStream(uri).use { inputStream ->
                        if (inputStream == null || outputStream == null) throw IOException("Failed to open streams")
                        inputStream.copyTo(outputStream)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                }
            } else {
                throw IOException("Failed to create file in Downloads")
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ReceivedMediaThumbnail(
    uriString: String,
    type: WallpaperType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            WallpaperType.VIDEO -> {
                val thumbnail by produceState<android.graphics.Bitmap?>(null, uriString) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            android.media.MediaMetadataRetriever().run {
                                setDataSource(uriString.toUri().path)
                                val bmp = getFrameAtTime(0)
                                release()
                                bmp
                            }
                        }.getOrNull()
                    }
                }
                thumbnail?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(26.dp)
                )
            }

            WallpaperType.GIF -> {
                val gifLoader = remember {
                    coil.ImageLoader.Builder(context)
                        .components { add(coil.decode.ImageDecoderDecoder.Factory()) }
                        .build()
                }
                AsyncImage(
                    model = uriString,
                    imageLoader = gifLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                AsyncImage(
                    model = uriString,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topEnd = 6.dp, bottomStart = 10.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = when (type) {
                    WallpaperType.GIF -> "GIF"
                    WallpaperType.VIDEO -> "VID"
                    else -> "IMG"
                },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (type == WallpaperType.GIF) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color(0xFFFF9800).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(topEnd = 10.dp, bottomStart = 6.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "EXPERIMENTAL",
                    color = Color.White,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ReceivedThemeCard(
    theme: ColorTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (pressScale, pressOffsetY) = onPressScaleAndOffset(isPressed)

    val gradient = if (theme.isSolid) {
        Brush.linearGradient(listOf(theme.primaryColor, theme.primaryColor))
    } else {
        Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor))
    }

    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = modifier
                .offset(y = pressOffsetY)
                .scale(pressScale)
                .pressWithHaptic(
                    onClick = onClick,
                    haptic = haptic,
                    onPressChange = { isPressed = it }
                )
                .width(120.dp)
                .height(80.dp)
                .scale(animatedFocusedScale(isFocused))
                .background(brush = gradient, shape = RoundedCornerShape(12.dp))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .focusable()
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 8.dp, y = (-8).dp)
                .size(22.dp)
                .background(color = Color(0xFFE53935).copy(alpha = 0.9f), shape = CircleShape)
                .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.6f), shape = CircleShape)
                .clip(CircleShape)
                .clickable(
                    onClick = onDelete,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.custom_theme_delete),
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
