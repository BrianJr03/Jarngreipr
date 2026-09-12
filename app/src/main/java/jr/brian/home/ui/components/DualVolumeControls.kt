package jr.brian.home.ui.components

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.ThorVolume

/**
 * Dual volume controls for AYN Thor's two independent screens.
 *
 * - Top screen: STREAM_MUSIC via AudioManager.
 * - Bottom screen: `Settings.System.secondary_screen_volume_level` (and the
 *   `_for_headphones` variant while a wired headset is connected). Writing
 *   requires the user-grantable `WRITE_SETTINGS` special access — the inline
 *   prompt below launches the system screen that grants it.
 */
@Composable
fun DualVolumeControls(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    tintColor: Color = Color.DarkGray
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var primaryVolume by remember { mutableFloatStateOf(0f) }
    var secondaryVolume by remember { mutableFloatStateOf(ThorVolume.DEFAULT.toFloat()) }

    var canWriteSettings by remember { mutableStateOf(ThorVolume.canWriteSettings(context)) }
    var headphonesConnected by remember { mutableStateOf(ThorVolume.isWiredHeadsetConnected(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canWriteSettings = ThorVolume.canWriteSettings(context)
                headphonesConnected = ThorVolume.isWiredHeadsetConnected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isVisible, headphonesConnected) {
        if (isVisible) {
            canWriteSettings = ThorVolume.canWriteSettings(context)
            primaryVolume = ThorVolume.getTop(context).toFloat()
            secondaryVolume = ThorVolume.getBottom(context, headphonesConnected).toFloat()
        }
    }

    DisposableEffect(context, headphonesConnected) {
        val primaryVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                primaryVolume = ThorVolume.getTop(context).toFloat()
            }
        }

        val secondaryVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                secondaryVolume = ThorVolume.getBottom(context, headphonesConnected).toFloat()
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                primaryVolumeObserver
            )
            context.contentResolver.registerContentObserver(
                ThorVolume.bottomVolumeUri(headphones = false),
                false,
                secondaryVolumeObserver
            )
            context.contentResolver.registerContentObserver(
                ThorVolume.bottomVolumeUri(headphones = true),
                false,
                secondaryVolumeObserver
            )
        } catch (e: Exception) {
            Log.e("DualVolumeControls", "Failed to register volume observer", e)
        }

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(primaryVolumeObserver)
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
            },
            tintColor = tintColor
        )

        if (canWriteSettings) {
            VolumeSlider(
                label = stringResource(R.string.volume_bottom_screen),
                volume = secondaryVolume,
                maxVolume = ThorVolume.MAX.toFloat(),
                onVolumeChange = { newVolume ->
                    secondaryVolume = newVolume
                    ThorVolume.setBottom(context, newVolume.toInt(), headphonesConnected)
                },
                tintColor = tintColor
            )
        } else {
            WriteSettingsPromptCard(
                onGrant = {
                    context.startActivity(ThorVolume.manageWriteSettingsIntent(context))
                }
            )
        }
    }
}

@Composable
private fun WriteSettingsPromptCard(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.volume_bottom_write_settings_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.volume_bottom_write_settings_body),
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimaryColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.volume_bottom_write_settings_action),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VolumeSlider(
    label: String,
    volume: Float,
    maxVolume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    tintColor: Color = Color.DarkGray
) {
    var tempVolume by remember(volume) { mutableFloatStateOf(volume) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = tintColor,
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
                    tint = tintColor
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
                    thumbColor = tintColor,
                    activeTrackColor = tintColor,
                    inactiveTrackColor = tintColor.copy(alpha = tintColor.alpha * 0.3f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.volume_up_description),
                tint = tintColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${tempVolume.toInt()}",
                color = tintColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )
        }
    }
}
