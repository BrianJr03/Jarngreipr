package jr.brian.home.ui.animations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.math.PI



// ─────────────────────────────────────────────────────────────────────────────
// 7. Stack  ─ pages pile up behind the current one like a deck of cards
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerStackTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            // Current page slides left and off
            translationX = size.width * offset
            alpha = lerp(1f, 0f, absOffset)
        } else {
            // Pages behind: stacked, slightly smaller, don't move
            val scale = lerp(0.85f, 1f, 1f - absOffset.coerceIn(0f, 1f))
            val verticalOffset = lerp(0f, 24f * density, absOffset.coerceIn(0f, 1f))
            scaleX = scale
            scaleY = scale
            translationY = verticalOffset
            alpha = lerp(0.4f, 0.85f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 8. Accordion  ─ pages squash inward then expand outward at the fold
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerAccordionTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            // Outgoing: squash toward the right edge
            transformOrigin = TransformOrigin(1f, 0.5f)
            scaleX = lerp(1f, 0f, absOffset)
            alpha = lerp(1f, 0f, absOffset)
        } else {
            // Incoming: expand from the left edge
            transformOrigin = TransformOrigin(0f, 0.5f)
            scaleX = lerp(0f, 1f, 1f - absOffset)
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 9. Flip Vertical  ─ pages flip over a horizontal axis (top-to-bottom)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerFlipVerticalTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        cameraDistance = 30 * density

        if (offset <= 0f) {
            transformOrigin = TransformOrigin(0.5f, 0f)
            rotationX = lerp(0f, -90f, absOffset)
            alpha = lerp(1f, 0.2f, absOffset)
        } else {
            transformOrigin = TransformOrigin(0.5f, 1f)
            rotationX = lerp(90f, 0f, 1f - absOffset)
            alpha = lerp(0.2f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 10. Carousel  ─ off-screen pages are visible at small scale on the sides
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerCarouselTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        val scale = lerp(0.7f, 1f, 1f - absOffset)
        scaleX = scale
        scaleY = scale
        alpha = lerp(0.4f, 1f, 1f - absOffset)
        // Compress translation so neighbours peek in from the sides
        translationX = size.width * offset * 0.75f
    }


// ─────────────────────────────────────────────────────────────────────────────
// 11. Warp / Zoom Punch  ─ page zooms way in then snaps to normal size
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerWarpTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            // Outgoing: zoom out as it exits
            val scale = lerp(1f, 2.5f, absOffset)
            scaleX = scale; scaleY = scale
            alpha = lerp(1f, 0f, absOffset)
        } else {
            // Incoming: starts huge then snaps to 1f
            val scale = lerp(2.5f, 1f, 1f - absOffset)
            scaleX = scale; scaleY = scale
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 12. Newspaper Spin  ─ pages spin and scale like a headline flying in
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerNewspaperTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            rotationZ = lerp(0f, -180f, absOffset)
            val scale = lerp(1f, 0f, absOffset)
            scaleX = scale; scaleY = scale
            alpha = lerp(1f, 0f, absOffset)
        } else {
            rotationZ = lerp(180f, 0f, 1f - absOffset)
            val scale = lerp(0f, 1f, 1f - absOffset)
            scaleX = scale; scaleY = scale
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 13. Elastic Overshoot  ─ incoming page bounces past then settles
//     Best paired with a Spring animationSpec on the pager scroll
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerElasticTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        // Elastic curve: dampened sine overshoot
        val elastic = { t: Float ->
            if (t == 0f || t == 1f) t
            else {
                val c = 2f * PI.toFloat()
                1f + ((-2f).pow(10f * (t - 1f))) * sin((t - 1.075f) * c / 0.3f)
            }
        }

        if (offset <= 0f) {
            translationX = size.width * offset
            alpha = lerp(1f, 0f, absOffset)
        } else {
            // Apply elastic easing to the incoming translation
            val eased = 1f - elastic(1f - absOffset)
            translationX = size.width * eased
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }

// pow extension used by elastic
private fun Float.pow(exp: Float): Float = Math.pow(this.toDouble(), exp.toDouble()).toFloat()


// ─────────────────────────────────────────────────────────────────────────────
// 14. Shutter  ─ page splits into top/bottom halves that open like blinds
// ─────────────────────────────────────────────────────────────────────────────
//  NOTE: Requires wrapping the pager content in TWO overlapping composables
//  clipped to top and bottom halves. Example wrapper below.
//
//  @Composable
//  fun ShutterPage(pagerState: PagerState, page: Int, content: @Composable () -> Unit) {
//      Box(Modifier.fillMaxSize()) {
//          // Bottom half
//          Box(Modifier
//              .fillMaxWidth()
//              .fillMaxHeight(0.5f)
//              .align(Alignment.BottomCenter)
//              .clip(RectangleShape)
//              .pagerShutterTransition(pagerState, page, isTop = false)
//          ) { content() }
//          // Top half
//          Box(Modifier
//              .fillMaxWidth()
//              .fillMaxHeight(0.5f)
//              .align(Alignment.TopCenter)
//              .clip(RectangleShape)
//              .pagerShutterTransition(pagerState, page, isTop = true)
//          ) { content() }
//      }
//  }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerShutterTransition(
    pagerState: PagerState,
    page: Int,
    isTop: Boolean
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        val direction = if (isTop) -1f else 1f

        if (offset <= 0f) {
            translationY = size.height * absOffset * direction
            alpha = lerp(1f, 0f, absOffset)
        } else {
            translationY = size.height * (1f - absOffset) * direction
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 15. Glide Up  ─ pages slide vertically (bottom-to-top), great for feeds
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerGlideUpTransition(pagerState: PagerState, page: Int): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            translationY = -size.height * absOffset
            alpha = lerp(1f, 0f, absOffset)
        } else {
            translationY = size.height * absOffset
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }

// Helper: normalised offset for the current page (-1f … 1f)
@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.offsetForPage(page: Int): Float =
    (currentPage - page) + currentPageOffsetFraction


// ─────────────────────────────────────────────────────────────────────────────
// 1. Slide  ─ simple horizontal translation (enhances the default behaviour)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerSlideTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        translationX = size.width * offset
        alpha = lerp(0.6f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
    }


// ─────────────────────────────────────────────────────────────────────────────
// 2. Fade  ─ cross-dissolve between pages
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerFadeTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        alpha = lerp(0f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
    }


// ─────────────────────────────────────────────────────────────────────────────
// 3. Cube  ─ pages rotate around a shared vertical edge like a 3D cube
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerCubeTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        cameraDistance = 20 * density

        if (offset < 0f) {
            // This page is to the LEFT of the current page → rotate outward on the right edge
            transformOrigin = TransformOrigin(1f, 0.5f)
            rotationY = 90f * offset.absoluteValue     // 0° → 90° as it leaves
        } else {
            // This page is to the RIGHT → rotate in from the left edge
            transformOrigin = TransformOrigin(0f, 0.5f)
            rotationY = -90f * offset.absoluteValue    // -90° → 0° as it arrives
        }

        alpha = lerp(0.4f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f))
    }


// ─────────────────────────────────────────────────────────────────────────────
// 4. Depth  ─ outgoing page recedes; incoming page zooms forward (parallax)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerDepthTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            // Current / leaving page: scale down and fade
            scaleX = lerp(1f, 0.75f, absOffset)
            scaleY = lerp(1f, 0.75f, absOffset)
            alpha = lerp(1f, 0f, absOffset)
        } else {
            // Incoming page: start small from behind, translate in from the right
            scaleX = lerp(0.75f, 1f, 1f - absOffset)
            scaleY = lerp(0.75f, 1f, 1f - absOffset)
            translationX = size.width * offset
            alpha = lerp(0f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 5. Tinder Card Swipe  ─ page fans out and rotates away like a swiped card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerTinderTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)

        if (offset <= 0f) {
            // The card being swiped away
            transformOrigin = TransformOrigin(0.5f, 1f)          // pivot at bottom-centre
            rotationZ = lerp(0f, -25f, absOffset) * if (offset < 0) 1f else -1f
            translationX = size.width * offset * 1.5f
            alpha = lerp(1f, 0f, absOffset * 0.8f)
        } else {
            // The card waiting underneath: scale up as the top card leaves
            scaleX = lerp(0.88f, 1f, 1f - absOffset)
            scaleY = lerp(0.88f, 1f, 1f - absOffset)
            alpha = lerp(0.5f, 1f, 1f - absOffset)
        }
    }


// ─────────────────────────────────────────────────────────────────────────────
// 6. Book Page Turn  ─ pages fold around a vertical spine like a real book
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pagerBookTransition(
    pagerState: PagerState,
    page: Int
): Modifier =
    graphicsLayer {
        val offset = pagerState.offsetForPage(page)
        cameraDistance = 30 * density

        when {
            offset < 0f -> {
                // Page folding away to the LEFT  (right edge is the spine)
                transformOrigin = TransformOrigin(0f, 0.5f)
                rotationY = lerp(0f, 90f, offset.absoluteValue.coerceIn(0f, 1f))
                alpha = lerp(1f, 0.2f, offset.absoluteValue.coerceIn(0f, 1f))
            }

            offset > 0f -> {
                // Page unfolding from the RIGHT (left edge is the spine)
                transformOrigin = TransformOrigin(1f, 0.5f)
                rotationY = lerp(0f, -90f, offset.coerceIn(0f, 1f))
                alpha = lerp(1f, 0.2f, offset.coerceIn(0f, 1f))
            }
        }
    }