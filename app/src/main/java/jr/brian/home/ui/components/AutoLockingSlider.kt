package jr.brian.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private const val AUTO_LOCK_MILLIS = 3000L

@Composable
fun AutoLockingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    var unlocked by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(unlocked, interactionTick) {
        if (unlocked) {
            delay(AUTO_LOCK_MILLIS)
            unlocked = false
        }
    }

    Box(modifier = modifier) {
        Slider(
            value = value,
            onValueChange = {
                if (unlocked) {
                    onValueChange(it)
                    interactionTick++
                }
            },
            onValueChangeFinished = {
                if (unlocked) {
                    onValueChangeFinished?.invoke()
                    interactionTick++
                }
            },
            enabled = unlocked,
            valueRange = valueRange,
            steps = steps,
            colors = colors
        )
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            unlocked = true
                            interactionTick++
                        }
                    )
            )
        }
    }
}
