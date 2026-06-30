package jr.brian.home.esde.ui.frontend

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.shape.RoundedCornerShape
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

        const val FocusSpringDamping = Spring.DampingRatioNoBouncy
        const val FocusSpringStiffness = Spring.StiffnessMedium
    }

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
