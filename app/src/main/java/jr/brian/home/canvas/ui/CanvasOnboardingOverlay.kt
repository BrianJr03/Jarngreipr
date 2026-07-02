package jr.brian.home.canvas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.onboarding.OnboardingStep
import jr.brian.home.ui.components.onboarding.OnboardingTooltip
import kotlinx.coroutines.delay

const val CANVAS_ONBOARDING_TARGET_MENU_BUTTON = "canvas_menu_button"
const val CANVAS_ONBOARDING_TARGET_CANVAS_AREA = "canvas_area"

/**
 * First-run guided tour for the Unified Canvas tab. Mirrors
 * [jr.brian.home.ui.components.onboarding.HeaderOnboardingOverlay]: opaque
 * black scrim with the current step's target redrawn on top of it and an
 * [OnboardingTooltip] centered. Unlike the header overlay, this one exposes a
 * Skip control so a user who lands on Canvas mid-configuration can dismiss
 * without walking through both steps.
 */
@Composable
fun CanvasOnboardingOverlay(
    steps: List<OnboardingStep>,
    currentStep: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    menuButtonCoordinates: LayoutCoordinates?,
    canvasAreaCoordinates: LayoutCoordinates?,
    menuButtonContent: @Composable () -> Unit,
    canvasAreaContent: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        delay(10)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible && currentStep < steps.size,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val coordinates = when (steps[currentStep].targetId) {
                CANVAS_ONBOARDING_TARGET_MENU_BUTTON -> menuButtonCoordinates
                CANVAS_ONBOARDING_TARGET_CANVAS_AREA -> canvasAreaCoordinates
                else -> null
            }

            val content: @Composable () -> Unit =
                when (steps[currentStep].targetId) {
                    CANVAS_ONBOARDING_TARGET_MENU_BUTTON -> menuButtonContent
                    CANVAS_ONBOARDING_TARGET_CANVAS_AREA -> canvasAreaContent
                    else -> {
                        {}
                    }
                }

            if (coordinates != null) {
                val positionInRoot = coordinates.positionInRoot()
                val size = coordinates.size

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = positionInRoot.x.toInt(),
                                y = positionInRoot.y.toInt()
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

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingTooltip(
                    title = steps[currentStep].title,
                    description = steps[currentStep].description,
                    isLastStep = currentStep == steps.size - 1,
                    onNext = {
                        if (currentStep < steps.size - 1) {
                            onNext()
                        } else {
                            onComplete()
                        }
                    }
                )
            }

            CanvasOnboardingSkipButton(
                onSkip = onComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun CanvasOnboardingSkipButton(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onSkip, modifier = modifier) {
        Text(
            text = stringResource(R.string.onboarding_skip),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
