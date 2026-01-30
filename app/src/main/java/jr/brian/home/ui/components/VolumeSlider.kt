package jr.brian.home.ui.components

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    isVisible: Boolean = true,
    showPermissionWarning: Boolean = false,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var primaryVolume by remember { mutableFloatStateOf(0f) }
    var secondaryVolume by remember { mutableFloatStateOf(0f) }
    
    // Check if we have permission to modify system settings (needed for secondary volume)
    var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
    var canWriteSecureSettings by remember { mutableStateOf(false) }
    
    // Recheck permission and load volumes when the component becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            canWriteSettings = Settings.System.canWrite(context)
            
            // Check if we can write to Global settings (test by attempting to read/write)
            canWriteSecureSettings = try {
                // Try to write and immediately read back a test value
                val testKey = "test_dual_volume_permission"
                Settings.Global.putInt(context.contentResolver, testKey, 1)
                Settings.Global.getInt(context.contentResolver, testKey) == 1
            } catch (e: Exception) {
                Log.w("DualVolumeControls", "Cannot write to Global settings: ${e.message}")
                false
            }
            
            Log.d("DualVolumeControls", "Permissions - WRITE_SETTINGS: $canWriteSettings, WRITE_SECURE_SETTINGS: $canWriteSecureSettings")
            
            // Load primary volume
            primaryVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            
            // Try to load secondary volume from Global settings (Ayn Thor uses this)
            secondaryVolume = try {
                Settings.Global.getInt(context.contentResolver, "secondary_screen_volume_level").toFloat()
            } catch (e: Settings.SettingNotFoundException) {
                // Fallback to primary volume if not found
                Log.w("DualVolumeControls", "secondary_screen_volume_level not found in Global settings")
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
                } catch (e: Settings.SettingNotFoundException) {
                    Log.w("DualVolumeControls", "secondary_screen_volume_level not found")
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
            Log.w("DualVolumeControls", "Could not register secondary volume observer", e)
        }

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(secondaryVolumeObserver)
            } catch (e: Exception) {
                Log.w("DualVolumeControls", "Error unregistering secondary volume observer", e)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        VolumeSlider(
            label = "Top Screen Volume",
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
                    Log.d("DualVolumeControls", "Primary volume set to: ${newVolume.toInt()}/${maxVolume}")
                } catch (e: Exception) {
                    Log.e("DualVolumeControls", "Error setting primary volume", e)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VolumeSlider(
            label = "Bottom Screen Volume",
            volume = secondaryVolume,
            maxVolume = maxVolume.toFloat(),
            onVolumeChange = { newVolume ->
                secondaryVolume = newVolume
                if (!canWriteSecureSettings) {
                    Log.w("DualVolumeControls", "Cannot set secondary volume - WRITE_SECURE_SETTINGS permission not granted")
                    return@VolumeSlider
                }
                
                try {
                    // Use Settings.Global for Ayn Thor's secondary screen volume
                    Settings.Global.putInt(
                        context.contentResolver,
                        "secondary_screen_volume_level",
                        newVolume.toInt()
                    )
                    Log.d("DualVolumeControls", "Secondary volume set to: ${newVolume.toInt()}/${maxVolume}")
                } catch (e: Exception) {
                    Log.e("DualVolumeControls", "Error setting secondary volume - is WRITE_SECURE_SETTINGS granted?", e)
                }
            }
        )
    }
}

@Composable
fun VolumeSlider(
    label: String,
    volume: Float,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Volume Down",
                tint = Color.DarkGray
            )
            
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
                contentDescription = "Volume Up",
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
