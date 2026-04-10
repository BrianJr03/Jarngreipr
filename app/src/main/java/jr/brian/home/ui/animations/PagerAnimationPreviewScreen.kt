package jr.brian.home.ui.animations

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AnimationPreset(
    val name: String,
    val tag: String,
    val description: String,
    val modifier: (PagerState, Int) -> Modifier
)

const val TAB_TRANSITION_NONE = ""

@OptIn(ExperimentalFoundationApi::class)
val allAnimationPresets: List<AnimationPreset> = listOf(
    AnimationPreset("None", "Default",
        "No special effect — standard pager slide behaviour."
    ) { _, _ -> Modifier },

    AnimationPreset("Fade", "Smooth",
        "Pages cross-fade — great for content-heavy or image-based screens."
    ) { state, page -> Modifier.pagerFadeTransition(state, page) },

    AnimationPreset("Cube", "3D",
        "Pages rotate around a shared vertical edge like a 3D cube."
    ) { state, page -> Modifier.pagerCubeTransition(state, page) },

    AnimationPreset("Book", "Flip",
        "Pages fold around a vertical spine like a real book."
    ) { state, page -> Modifier.pagerBookTransition(state, page) },

    AnimationPreset("Accordion", "Fold",
        "Pages squash and expand at their shared edge like an accordion."
    ) { state, page -> Modifier.pagerAccordionTransition(state, page) },

    AnimationPreset("Flip Vertical", "3D",
        "Pages tumble over a horizontal axis."
    ) { state, page -> Modifier.pagerFlipVerticalTransition(state, page) },

    AnimationPreset("Warp", "Zoom punch",
        "Outgoing page explodes out; incoming warps in from oversized."
    ) { state, page -> Modifier.pagerWarpTransition(state, page) },

    AnimationPreset("Newspaper", "Spin",
        "Pages spin 180° and scale, like a headline spiralling in."
    ) { state, page -> Modifier.pagerNewspaperTransition(state, page) },

    AnimationPreset("Glide Up", "Vertical",
        "Pages glide vertically — outgoing flies up, incoming rises."
    ) { state, page -> Modifier.pagerGlideUpTransition(state, page) },
)

private val previewPageColors = listOf(
    Color(0xFFCECBF6) to Color(0xFF3C3489), // purple
    Color(0xFF9FE1CB) to Color(0xFF085041), // teal
    Color(0xFFF5C4B3) to Color(0xFF993C1D), // coral
    Color(0xFFB5D4F4) to Color(0xFF185FA5), // blue
    Color(0xFFC0DD97) to Color(0xFF3B6D11), // green
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnimationPresetCard(
    preset: AnimationPreset,
    isSelected: Boolean = false,
    onSelected: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(pageCount = { previewPageColors.size })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onSelected != null) Modifier.clickable { onSelected() } else Modifier
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagChip(preset.tag)
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Pager preview
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) { page ->
                val (bg, fg) = previewPageColors[page % previewPageColors.size]
                PreviewPage(
                    pageIndex = page,
                    background = bg,
                    foreground = fg,
                    modifier = preset.modifier(pagerState, page)
                )
            }

            // Dot indicators
            PageDots(
                pageCount = previewPageColors.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun PreviewPage(
    pageIndex: Int,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Page ${pageIndex + 1}",
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "← swipe →",
                color = foreground.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 8.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
            )
        }
    }
}