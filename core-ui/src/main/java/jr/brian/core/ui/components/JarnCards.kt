package jr.brian.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.displayName
import jr.brian.core.ui.theme.resolvedColors

@Composable
fun JarnEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = JarnTheme.colors
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(JarnTheme.space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        icon?.let { JarnIcon(it, colors.textMid) }
        JarnText(title, JarnTheme.type.title, colors.textHigh, maxLines = 2, textAlign = TextAlign.Center)
        JarnText(message, JarnTheme.type.body, colors.textMid, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            JarnButton(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun JarnLookCard(
    look: JarnLook,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    art: (@Composable BoxScope.() -> Unit)? = null,
) {
    JarnChoiceCard(
        name = look.displayName(),
        chosen = chosen,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
    ) { JarnLookPreview(look.resolvedColors, art, Modifier.weight(1f)) }
}

/**
 * A pickable card in the look cards' style: [preview] on top, then [name] with a check when
 * [chosen], then an optional [supporting] line.
 */
@Composable
fun JarnChoiceCard(
    name: String,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    height: Dp = 156.dp,
    interactionSource: MutableInteractionSource? = null,
    preview: @Composable ColumnScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.mdShape
    val chosenState = stringResource(
        if (chosen) R.string.jarn_state_selected else R.string.jarn_state_not_selected,
    )
    val chosenBorder = if (chosen) Modifier.border(2.dp, JarnTheme.colors.accent, shape) else Modifier
    Column(
        modifier = modifier
            .semantics {
                selected = chosen
                stateDescription = chosenState
            }
            .jarnInteractive(shape, source, role = Role.RadioButton, focusStyle = JarnFocusStyle.Scale, onClick = onClick)
            .height(height)
            .background(JarnTheme.colors.surface, shape)
            .then(chosenBorder)
            .padding(JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        preview()
        JarnLookCardFooter(name, chosen)
        supporting?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 2) }
    }
}

@Composable
private fun JarnLookPreview(
    preview: JarnColors,
    art: (@Composable BoxScope.() -> Unit)?,
    modifier: Modifier,
) {
    val shape = JarnTheme.radius.smShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(preview.bg),
    ) {
        art?.invoke(this)
        JarnLookSwatches(preview, Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun JarnLookSwatches(preview: JarnColors, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(JarnTheme.space.sm),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        listOf(preview.surface, preview.raised, preview.textHigh, preview.accent).forEach { swatch ->
            JarnSwatch(swatch, preview.line)
        }
    }
}

@Composable
private fun JarnSwatch(color: Color, outline: Color) {
    Box(
        Modifier
            .size(16.dp)
            .background(color, CircleShape)
            .border(1.dp, outline, CircleShape),
    )
}

@Composable
private fun JarnLookCardFooter(name: String, chosen: Boolean) {
    val colors = JarnTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(name, JarnTheme.type.labelStrong, colors.textHigh, Modifier.weight(1f), maxLines = 1)
        if (chosen) JarnCheckGlyph(colors.accent)
    }
}

@Composable
fun JarnScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(JarnTheme.colors.bg), content = content)
}
