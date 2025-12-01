package jr.brian.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.model.WakeMethod
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager

@Composable
fun BlackScreen(
    modifier: Modifier = Modifier,
    onPowerOn: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val powerSettingsManager = LocalPowerSettingsManager.current
    val wakeMethod by powerSettingsManager.wakeMethod.collectAsStateWithLifecycle()

    val defaultConfig = LocalViewConfiguration.current
    val customViewConfiguration = remember(defaultConfig) {
        object : ViewConfiguration {
            override val longPressTimeoutMillis: Long = 1000L
            override val doubleTapTimeoutMillis: Long = defaultConfig.doubleTapTimeoutMillis
            override val doubleTapMinTimeMillis: Long = defaultConfig.doubleTapMinTimeMillis
            override val touchSlop: Float = defaultConfig.touchSlop
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides customViewConfiguration
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        onPowerOn()
                        true
                    } else {
                        false
                    }
                }
                .then(
                    when (wakeMethod) {
                        WakeMethod.SINGLE_TAP -> {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onPowerOn()
                            }
                        }

                        WakeMethod.DOUBLE_TAP -> {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onPowerOn() }
                                )
                            }
                        }

                        WakeMethod.LONG_PRESS -> {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { onPowerOn() }
                                )
                            }
                        }
                    }
                )
        )
    }
}
