package jr.brian.home.ui.components

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R

/**
 * Dual volume controls for devices with multiple screens (e.g., Ayn Thor)
 * Manages both primary (top screen) and secondary (bottom screen) volume controls
 * 
 * Features:
 * - Primary volume control (always works)
 * - Secondary volume control (uses Settings.Global, may require WRITE_SECURE_SETTINGS)
 * - Automatic permission checking and request UI
 * - Real-time volume observer for secondary screen
 * 
 * Note: Secondary volume uses Settings.Global which may require granting permission via ADB:
 * adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS
 */
@Composable
fun DualVolumeControls(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var primaryVolume by remember { mutableFloatStateOf(0f) }
    var secondaryVolume by remember { mutableFloatStateOf(0f) }
    
    var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var canWriteSecureSettings by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            canWriteSettings = Settings.System.canWrite(context)
            
            canWriteSecureSettings = try {
                val testKey = "test_dual_volume_permission"
                Settings.Global.putInt(context.contentResolver, testKey, 1)
                Settings.Global.getInt(context.contentResolver, testKey) == 1
            } catch (_: Exception) {
                false
            }
            
            primaryVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            
            secondaryVolume = try {
                Settings.Global.getInt(context.contentResolver, "secondary_screen_volume_level").toFloat()
            } catch (_: Settings.SettingNotFoundException) {
                primaryVolume
            }
        }
    }

    DisposableEffect(context) {
        val secondaryVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                try {
                    secondaryVolume = Settings.Global.getInt(
                        context.contentResolver,
                        "secondary_screen_volume_level"
                    ).toFloat()
                } catch (_: Settings.SettingNotFoundException) {
                    // Silently ignore - setting may not exist on all devices
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("secondary_screen_volume_level"),
                false,
                secondaryVolumeObserver
            )
        } catch (e: Exception) {
            Log.e("DualVolumeControls", "Failed to register volume observer", e)
        }

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(secondaryVolumeObserver)
            } catch (e: Exception) {
                Log.e("DualVolumeControls", "Failed to unregister volume observer", e)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        VolumeSlider(
            label = stringResource(R.string.volume_top_screen),
            volume = primaryVolume,
            maxVolume = maxVolume.toFloat(),
            onVolumeChange = { newVolume ->
                primaryVolume = newVolume
                try {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        newVolume.toInt(),
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (e: Exception) {
                    Log.e("DualVolumeControls", "Failed to set primary volume", e)
                }
            }
        )

//        TODO: Ask AYN for the correct way to control the bottom screen's volume
//        Spacer(modifier = Modifier.height(16.dp))
//        VolumeSlider(
//            label = stringResource(R.string.volume_bottom_screen),
//            volume = secondaryVolume,
//            maxVolume = maxVolume.toFloat(),
//            onVolumeChange = { newVolume ->
//                secondaryVolume = newVolume
//                if (!canWriteSecureSettings) {
//                    return@VolumeSlider
//                }
//
//                try {
//                    Settings.Global.putInt(
//                        context.contentResolver,
//                        "secondary_screen_volume_level",
//                        newVolume.toInt()
//                    )
//                } catch (e: Exception) {
//                    Log.e("DualVolumeControls", "Failed to set secondary volume", e)
//                }
//            }
//        )
    }
}

@Composable
fun VolumeSlider(
    label: String,
    volume: Float,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var tempVolume by remember(volume) { mutableFloatStateOf(volume) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.DarkGray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = stringResource(R.string.volume_down_description),
                    tint = Color.DarkGray
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Slider(
                value = tempVolume,
                onValueChange = { newValue ->
                    tempVolume = newValue
                    onVolumeChange(newValue)
                },
                valueRange = 0f..maxVolume,
                steps = if (maxVolume.toInt() > 1) maxVolume.toInt() - 1 else 0,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Gray,
                    activeTrackColor = Color.Gray,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.volume_up_description),
                tint = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "${tempVolume.toInt()}",
                color = Color.DarkGray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )
        }
    }
}
