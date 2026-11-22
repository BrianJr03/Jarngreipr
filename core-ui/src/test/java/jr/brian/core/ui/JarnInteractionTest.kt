package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun focus(requester: FocusRequester) {
        rule.runOnIdle { requester.requestFocus() }
        rule.waitForIdle()
    }

    @Test
    fun `gamepad A clicks once on release`() {
        var clicks = 0
        val requester = FocusRequester()
        rule.setContent {
            JarnTheme {
                JarnButton("go", { clicks++ }, Modifier.testTag("b").focusRequester(requester))
            }
        }
        focus(requester)
        rule.onNodeWithTag("b").performKeyInput { keyDown(Key.ButtonA) }
        rule.runOnIdle { assertEquals(0, clicks) }
        rule.onNodeWithTag("b").performKeyInput { keyUp(Key.ButtonA) }
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `disabled button stays focusable when asked but does not click`() {
        var clicks = 0
        val requester = FocusRequester()
        rule.setContent {
            JarnTheme {
                JarnButton(
                    label = "go",
                    onClick = { clicks++ },
                    modifier = Modifier.testTag("b").focusRequester(requester),
                    enabled = false,
                    focusableWhenDisabled = true,
                )
            }
        }
        focus(requester)
        rule.onNodeWithTag("b").assertIsFocused().assertIsNotEnabled()
        rule.onNodeWithTag("b").performKeyInput { pressKey(Key.ButtonA) }
        rule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun `disabled button is skipped by focus by default`() {
        val requester = FocusRequester()
        rule.setContent {
            JarnTheme {
                JarnButton("go", {}, Modifier.testTag("b").focusRequester(requester), enabled = false)
            }
        }
        focus(requester)
        rule.onNodeWithTag("b").assertIsNotFocused()
    }

    @Test
    fun `slider adjusts with left and right and holds focus at the ends`() {
        var value by mutableFloatStateOf(0.95f)
        val requester = FocusRequester()
        rule.setContent {
            JarnTheme {
                JarnSliderRow(
                    label = "level",
                    value = value,
                    valueText = "v",
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("s").focusRequester(requester),
                )
            }
        }
        focus(requester)
        rule.onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(1f, value, 0.001f) }
        rule.onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("s").assertIsFocused()
        rule.onNodeWithTag("s").performKeyInput { pressKey(Key.DirectionLeft) }
        rule.runOnIdle { assertEquals(0.95f, value, 0.001f) }
    }

    @Test
    fun `destructive dialog opens on cancel and B dismisses`() {
        var dismissed = 0
        var visible by mutableStateOf(true)
        rule.setContent {
            JarnTheme {
                JarnDialog(
                    visible = visible,
                    title = "t",
                    message = "m",
                    confirmLabel = "delete",
                    dismissLabel = "cancel",
                    onConfirm = {},
                    onDismiss = {
                        dismissed++
                        visible = false
                    },
                    destructive = true,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("cancel").assertIsFocused()
        rule.onNodeWithText("cancel").performKeyInput { pressKey(Key.ButtonB) }
        rule.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test
    fun `sheet traps focus and restores it on close`() {
        var open by mutableStateOf(false)
        val opener = FocusRequester()
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(400.dp)) {
                    Column {
                        JarnButton("open", { open = true }, Modifier.testTag("open").focusRequester(opener))
                    }
                    JarnSheet(visible = open, onDismiss = { open = false }, restoreFocusTo = opener) {
                        JarnButton("inside", {}, Modifier.testTag("inside"))
                    }
                }
            }
        }
        focus(opener)
        rule.onNodeWithTag("open").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("inside").assertIsFocused()
        rule.onNodeWithTag("inside").performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("inside").assertIsFocused()
        rule.onNodeWithTag("inside").performKeyInput { pressKey(Key.ButtonB) }
        rule.waitForIdle()
        rule.onNodeWithTag("open").assertIsFocused()
    }
}
