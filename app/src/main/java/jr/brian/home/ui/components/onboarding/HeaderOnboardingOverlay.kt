package jr.brian.home.ui.components.onboarding

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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun HeaderOnboardingOverlay(
    steps: List<OnboardingStep>,
    currentStep: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconCoordinates: LayoutCoordinates?,
    pageIndicatorsCoordinates: LayoutCoordinates?,
    trailingIconCoordinates: LayoutCoordinates?,
    leadingIconContent: @Composable () -> Unit,
    pageIndicatorsContent: @Composable () -> Unit,
    trailingIconContent: @Composable () -> Unit,
    keyboardCoordinates: LayoutCoordinates? = null,
    keyboardContent: @Composable (() -> Unit)? = null
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
                "leading_icon" -> leadingIconCoordinates
                "page_indicators" -> pageIndicatorsCoordinates
                "trailing_icon" -> trailingIconCoordinates
                "keyboard" -> keyboardCoordinates
                else -> null
            }

            val content: @Composable () -> Unit =
                if (steps[currentStep].targetId == "leading_icon") {
                    leadingIconContent
                } else if (steps[currentStep].targetId == "page_indicators") {
                    pageIndicatorsContent
                } else if (steps[currentStep].targetId == "trailing_icon") {
                    trailingIconContent
                } else if (steps[currentStep].targetId == "keyboard" && keyboardContent != null) {
                    keyboardContent
                } else {
                    {}
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
        }
    }
}
