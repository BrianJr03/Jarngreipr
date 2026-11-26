package jr.brian.home.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.JarnFeOffer
import jr.brian.home.ui.components.dialog.AppBottomSheet
import jr.brian.home.ui.components.settings.rememberTryJarnFe
import jr.brian.home.ui.meetjarn.MeetJarnContent
import jr.brian.home.ui.meetjarn.MeetJarnHero
import jr.brian.home.ui.meetjarn.heroAspect
import jr.brian.home.ui.meetjarn.loadMeetJarnContent
import jr.brian.home.ui.theme.ThemePrimaryColor

private const val TRY_JARN_HERO_SHARE = 0.5f
private const val TRY_JARN_HERO_MAX_HEIGHT_SHARE = 0.6f
private val TRY_JARN_HERO_FALLBACK_MAX_HEIGHT = 320.dp

/** True when this build offers Jarn FE. */
@Composable
fun rememberJarnFeOffered(): Boolean {
    val context = LocalContext.current
    return remember(context) { JarnFeOffer.isOffered(context) }
}

/** The compact Try Jarn button for a sheet header. It opens [TryJarnSheet] through [onClick]. */
@Composable
fun TryJarnHeaderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemePrimaryColor,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isFocused) BorderStroke(2.dp, Color.White) else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        interactionSource = interactionSource,
    ) {
        Text(
            text = stringResource(R.string.jarn_preview_try),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * The Meet Jarn hero, the pitch and a Try Jarn button, which takes focus first. Back closes only
 * this sheet through [onDismiss]; [onOpened] runs once Jarn opens. The hero sits beside the copy on
 * wide screens and above it otherwise, and the sheet scrolls when it doesn't fit.
 */
@Composable
fun TryJarnSheet(
    onDismiss: () -> Unit,
    onOpened: () -> Unit,
) {
    val context = LocalContext.current
    val content by produceState<MeetJarnContent?>(initialValue = null, context) {
        value = loadMeetJarnContent(context)
    }
    AppBottomSheet(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            val heroMaxHeight = if (constraints.hasBoundedHeight) {
                maxHeight * TRY_JARN_HERO_MAX_HEIGHT_SHARE
            } else {
                TRY_JARN_HERO_FALLBACK_MAX_HEIGHT
            }
            if (maxWidth > maxHeight) {
                TryJarnWideLayout(content, heroMaxHeight, onOpened)
            } else {
                TryJarnTallLayout(content, heroMaxHeight, onOpened)
            }
        }
    }
}

@Composable
private fun TryJarnWideLayout(content: MeetJarnContent?, heroMaxHeight: Dp, onOpened: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(TRY_JARN_HERO_SHARE), contentAlignment = Alignment.Center) {
            TryJarnHero(content, heroMaxHeight)
        }
        TryJarnCopy(
            onOpened = onOpened,
            modifier = Modifier
                .weight(1f - TRY_JARN_HERO_SHARE)
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun TryJarnTallLayout(content: MeetJarnContent?, heroMaxHeight: Dp, onOpened: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TryJarnHero(content, heroMaxHeight)
        TryJarnCopy(onOpened = onOpened, modifier = Modifier.fillMaxWidth())
    }
}

/** Renders nothing until the content has loaded; the bundled picture covers a library without art. */
@Composable
private fun TryJarnHero(content: MeetJarnContent?, maxHeight: Dp) {
    if (content == null) return
    MeetJarnHero(content, Modifier.widthIn(max = maxHeight * content.heroAspect()))
}

@Composable
private fun TryJarnCopy(onOpened: () -> Unit, modifier: Modifier = Modifier) {
    val tryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { tryFocus.requestFocus() } }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.jarn_preview_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.jarn_preview_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
        )
        Text(
            text = stringResource(R.string.jarn_preview_reassurance),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )
        TryJarnFeButton(
            onOpened = onOpened,
            modifier = Modifier
                .padding(top = 8.dp)
                .focusRequester(tryFocus),
        )
    }
}

@Composable
private fun TryJarnFeButton(
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tryJarnFe = rememberTryJarnFe(onOpened)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Button(
        onClick = tryJarnFe,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = ThemePrimaryColor,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isFocused) BorderStroke(2.dp, Color.White) else null,
        interactionSource = interactionSource,
    ) {
        Text(stringResource(R.string.jarn_preview_try))
    }
}
