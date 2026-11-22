package jr.brian.core.ui.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

internal const val JARN_DISABLED_ALPHA = 0.38f

fun Modifier.jarnClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = this
    .focusProperties { canFocus = enabled || focusableWhenDisabled }
    .then(JarnConfirmKeyElement(interactionSource, enabled, onClick))
    .semantics { if (!enabled) disabled() }
    .clickable(
        interactionSource = interactionSource,
        indication = null,
        role = role,
        onClick = { if (enabled) onClick() },
    )

fun Modifier.jarnInteractive(
    shape: Shape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    focusableWhenDisabled: Boolean = false,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = this
    .jarnFocus(shape, interactionSource, enabled)
    .jarnClickable(interactionSource, enabled, focusableWhenDisabled, role, onClick)
    .jarnDisabledAlpha(enabled)

fun Modifier.jarnDisabledAlpha(enabled: Boolean): Modifier =
    if (enabled) this else alpha(JARN_DISABLED_ALPHA)

private data class JarnConfirmKeyElement(
    val interactionSource: MutableInteractionSource,
    val enabled: Boolean,
    val onClick: () -> Unit,
) : ModifierNodeElement<JarnConfirmKeyNode>() {
    override fun create() = JarnConfirmKeyNode(interactionSource, enabled, onClick)

    override fun update(node: JarnConfirmKeyNode) {
        node.interactionSource = interactionSource
        node.enabled = enabled
        node.onClick = onClick
        if (!enabled) node.cancelPress()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "jarnConfirmKey"
        properties["enabled"] = enabled
    }
}

private class JarnConfirmKeyNode(
    var interactionSource: MutableInteractionSource,
    var enabled: Boolean,
    var onClick: () -> Unit,
) : Modifier.Node(), KeyInputModifierNode, FocusEventModifierNode {

    private var press: PressInteraction.Press? = null

    override fun onPreKeyEvent(event: KeyEvent): Boolean = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.key != Key.ButtonA) return false
        when (event.type) {
            KeyEventType.KeyDown -> beginPress(event)
            KeyEventType.KeyUp -> finishPress()
        }
        return true
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (!focusState.isFocused) cancelPress()
    }

    override fun onDetach() {
        cancelPress()
    }

    private fun beginPress(event: KeyEvent) {
        if (!enabled || press != null || event.nativeKeyEvent.repeatCount > 0) return
        press = PressInteraction.Press(Offset.Zero).also(interactionSource::tryEmit)
    }

    private fun finishPress() {
        val active = press ?: return
        press = null
        interactionSource.tryEmit(PressInteraction.Release(active))
        if (enabled) onClick()
    }

    fun cancelPress() {
        val active = press ?: return
        press = null
        interactionSource.tryEmit(PressInteraction.Cancel(active))
    }
}
