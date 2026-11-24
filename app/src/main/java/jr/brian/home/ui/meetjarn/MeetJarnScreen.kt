package jr.brian.home.ui.meetjarn

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jr.brian.home.R

/** Classic's colors for the screen, passed in so the screen doesn't depend on Classic's theme managers. */
@Immutable
data class MeetJarnColors(val primary: Color, val background: Color)

internal const val MEET_JARN_PRESS_MS = 80
private const val MEET_JARN_PRESS_SCALE = 0.97f
private const val MEET_JARN_HERO_SHARE = 0.55f
private const val MEET_JARN_EYEBROW_TINT = 0.45f
internal const val MEET_JARN_TRY_TAG = "meet_jarn_try"
internal const val MEET_JARN_NOT_NOW_TAG = "meet_jarn_not_now"

/**
 * Meet Jarn in Classic's own design: the hero on the left, the pitch and the two buttons on the
 * right (stacked on narrow screens). Try Jarn takes focus first, and B means Not now.
 */
@Composable
fun MeetJarnScreen(
    content: MeetJarnContent,
    colors: MeetJarnColors,
    onTry: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onNotNow)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onPreviewKeyEvent { event ->
                val isBack = event.key == Key.ButtonB || event.key == Key.Back || event.key == Key.Escape
                if (isBack && event.type == KeyEventType.KeyUp) onNotNow()
                isBack
            },
    ) {
        if (maxWidth > maxHeight) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MeetJarnHero(content, Modifier.weight(MEET_JARN_HERO_SHARE))
                MeetJarnCopy(content, colors, onTry, onNotNow, Modifier.weight(1f - MEET_JARN_HERO_SHARE))
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                MeetJarnHero(content)
                MeetJarnCopy(content, colors, onTry, onNotNow)
            }
        }
    }
}

@Composable
private fun MeetJarnCopy(
    content: MeetJarnContent,
    colors: MeetJarnColors,
    onTry: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.meet_jarn_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = lerp(colors.primary, Color.White, MEET_JARN_EYEBROW_TINT),
        )
        Text(
            text = stringResource(R.string.meet_jarn_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(
            text = stringResource(if (content.twoScreens) R.string.meet_jarn_body else R.string.meet_jarn_body_single),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
        )
        MeetJarnPoints(content, colors.primary)
        MeetJarnButtons(colors, onTry, onNotNow)
        Text(
            text = stringResource(R.string.meet_jarn_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MeetJarnPoints(content: MeetJarnContent, primary: Color) {
    val controller = if (content.twoScreens) R.string.meet_jarn_controller else R.string.meet_jarn_controller_single
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(content.library.lineRes, controller, R.string.meet_jarn_switch_back).forEach { line ->
            MeetJarnPoint(stringResource(line), primary)
        }
    }
}

@Composable
private fun MeetJarnPoint(text: String, primary: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).background(primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun MeetJarnButtons(colors: MeetJarnColors, onTry: () -> Unit, onNotNow: () -> Unit) {
    val tryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { tryFocus.requestFocus() } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MeetJarnButton(
            label = stringResource(R.string.meet_jarn_try),
            container = colors.primary,
            onClick = onTry,
            modifier = Modifier.focusRequester(tryFocus).testTag(MEET_JARN_TRY_TAG),
            weight = 1.3f,
        )
        MeetJarnButton(
            label = stringResource(R.string.meet_jarn_not_now),
            container = Color.White.copy(alpha = 0.1f),
            onClick = onNotNow,
            modifier = Modifier.testTag(MEET_JARN_NOT_NOW_TAG),
            weight = 1f,
        )
    }
}

/** Classic's button: 16dp corners, a white border while focused, and an 80ms press. */
@Composable
private fun RowScope.MeetJarnButton(
    label: String,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    weight: Float,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) MEET_JARN_PRESS_SCALE else 1f,
        animationSpec = tween(MEET_JARN_PRESS_MS),
        label = "meetJarnPress",
    )
    Button(
        onClick = onClick,
        modifier = modifier.weight(weight).graphicsLayer { scaleX = scale; scaleY = scale },
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
        interactionSource = interactions,
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}
