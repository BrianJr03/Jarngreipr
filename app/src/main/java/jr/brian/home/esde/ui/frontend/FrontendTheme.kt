package jr.brian.home.esde.ui.frontend

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FrontendTokens {

    object Spacing {
        val XXS = 4.dp
        val XS = 8.dp
        val S = 12.dp
        val M = 16.dp
        val L = 24.dp
        val XL = 32.dp
    }

    object Radius {
        val Badge = RoundedCornerShape(8.dp)
        val TileSmall = RoundedCornerShape(12.dp)
        val TileLarge = RoundedCornerShape(16.dp)
        val Panel = RoundedCornerShape(20.dp)
        val Overlay = RoundedCornerShape(24.dp)
    }

    object Alpha {
        const val Primary = 1f
        const val Secondary = 0.72f
        const val Tertiary = 0.45f
        const val Unfocused = 0.6f
        const val Faint = 0.3f
    }

    object Motion {
        val Easing = FastOutSlowInEasing

        const val PressMs = 100
        const val FocusMs = 180
        const val EntranceMs = 220
        const val ArtCrossfadeMs = 300
        const val ScrollMs = 320
        const val RouteMs = 360
        const val EntranceStaggerStepMs = 40
        const val FloatPeriodMs = 1800

        const val FocusSpringDamping = Spring.DampingRatioNoBouncy
        const val FocusSpringStiffness = Spring.StiffnessMedium
    }

    val FloatAmplitude = 3.dp

    object Type {
        val TileTitle = TextStyle(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.1.sp
        )
        val TileSubtitle = TextStyle(
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
        val Metadata = TextStyle(
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
internal fun focusFloatPhase(active: Boolean): Float {
    if (!active) return 0f
    val phase by rememberInfiniteTransition(label = "focusFloat").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = FrontendTokens.Motion.FloatPeriodMs,
                easing = FrontendTokens.Motion.Easing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusFloatPhase"
    )
    return phase
}

private var lastFocusHapticMs = 0L
private const val FOCUS_HAPTIC_MIN_INTERVAL_MS = 150L

internal fun View.emitFocusHapticIfReady() {
    val now = SystemClock.uptimeMillis()
    if (now - lastFocusHapticMs >= FOCUS_HAPTIC_MIN_INTERVAL_MS) {
        lastFocusHapticMs = now
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
