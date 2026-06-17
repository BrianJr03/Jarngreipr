package jr.brian.home.ui.components.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import kotlinx.coroutines.delay

@Composable
fun BoxScope.ShadeSliderOnboardingOverlay(
    cornerRadiusCoordinates: LayoutCoordinates?,
    opacityCoordinates: LayoutCoordinates?,
    cornerRadiusContent: @Composable () -> Unit,
    opacityContent: @Composable () -> Unit,
    onComplete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        delay(10)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = Modifier.matchParentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            SpotlightHighlight(
                coordinates = cornerRadiusCoordinates,
                density = density,
                content = cornerRadiusContent
            )

            SpotlightHighlight(
                coordinates = opacityCoordinates,
                density = density,
                content = opacityContent
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingTooltip(
                    title = stringResource(R.string.shade_slider_hint_title),
                    description = stringResource(R.string.shade_slider_hint_description),
                    isLastStep = true,
                    onNext = onComplete
                )
            }
        }
    }
}

@Composable
private fun SpotlightHighlight(
    coordinates: LayoutCoordinates?,
    density: Density,
    content: @Composable () -> Unit
) {
    if (coordinates == null) return
    val position = coordinates.positionInParent()
    val size = coordinates.size
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = position.x.toInt(),
                    y = position.y.toInt()
                )
            }
            .size(
                width = with(density) { size.width.toDp() },
                height = with(density) { size.height.toDp() }
            )
    ) {
        content()
    }
}
