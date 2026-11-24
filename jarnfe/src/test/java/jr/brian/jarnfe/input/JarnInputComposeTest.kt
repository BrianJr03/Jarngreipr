package jr.brian.jarnfe.input

import android.app.Application
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnInputComposeTest {

    @get:Rule
    val rule = createComposeRule()

    private val router = JarnInputRouter()

    private fun tap(code: Int) {
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0))
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 10))
    }

    @Test
    fun `nested layer sits above its parent and unregisters when removed`() {
        val log = mutableListOf<String>()
        var overlayOpen by mutableStateOf(true)
        rule.setContent {
            ProvideJarnInput(router, JarnScreen.Stage) {
                JarnInputLayer(bindings = { on(JarnAction.Back) { log += "parent" } }) {
                    if (overlayOpen) {
                        JarnInputLayer(
                            mode = JarnLayerMode.Modal,
                            bindings = {
                                on(JarnAction.Back) {
                                    log += "overlay"
                                    overlayOpen = false
                                }
                            },
                        )
                    }
                }
            }
        }
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_B) }
        rule.waitForIdle()
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_B) }
        rule.runOnIdle { assertEquals(listOf("overlay", "parent"), log) }
    }

    @Test
    fun `disabled layer takes no input`() {
        val log = mutableListOf<String>()
        var enabled by mutableStateOf(false)
        rule.setContent {
            ProvideJarnInput(router, JarnScreen.Stage) {
                JarnInputLayer(enabled = enabled, bindings = { on(JarnAction.Context) { log += "x" } })
            }
        }
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_SELECT) }
        rule.runOnIdle { assertEquals(emptyList<String>(), log) }
        enabled = true
        rule.waitForIdle()
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_SELECT) }
        rule.runOnIdle { assertEquals(listOf("x"), log) }
    }

    @Test
    fun `latest handler runs after recomposition`() {
        val log = mutableListOf<String>()
        var name by mutableStateOf("first")
        rule.setContent {
            val current = name
            ProvideJarnInput(router, JarnScreen.Stage) {
                JarnInputLayer(bindings = { on(JarnAction.Context) { log += current } })
            }
        }
        name = "second"
        rule.waitForIdle()
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_SELECT) }
        rule.runOnIdle { assertEquals(listOf("second"), log) }
    }

    @Test
    fun `hint bar follows the declared layers using string resources`() {
        var overlayOpen by mutableStateOf(false)
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    JarnInputLayer(
                        bindings = {
                            on(JarnAction.Confirm, R.string.jarnfe_hint_play) {}
                            on(JarnAction.Context, R.string.jarnfe_hint_context) {}
                        },
                    ) {
                        if (overlayOpen) {
                            JarnInputLayer(
                                mode = JarnLayerMode.Modal,
                                bindings = { on(JarnAction.Back, R.string.jarnfe_hint_back) {} },
                            )
                        }
                    }
                    JarnScreenHintBar()
                }
            }
        }
        rule.onNodeWithText("Play").assertExists()
        rule.onNodeWithText("Options").assertExists()
        overlayOpen = true
        rule.waitForIdle()
        rule.onNodeWithText("Back").assertExists()
        rule.onAllNodesWithText("Play").assertCountEquals(0)
    }

    @Test
    fun `inactive screen keeps its hints`() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Deck) {
                    JarnInputLayer(bindings = { on(JarnAction.Confirm, R.string.jarnfe_hint_confirm) {} })
                    JarnScreenHintBar()
                }
            }
        }
        rule.onNodeWithText("OK").assertExists()
        rule.runOnIdle { assertEquals(JarnScreen.Stage, router.activeScreen) }
    }
}
