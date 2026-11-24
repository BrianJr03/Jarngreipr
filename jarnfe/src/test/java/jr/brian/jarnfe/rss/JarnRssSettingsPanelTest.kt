package jr.brian.jarnfe.rss

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/** The clear dialog hands focus back through [clearFocus], so it must sit on the Clear row. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnRssSettingsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val data = Proxy.newProxyInstance(
        JarnRssData::class.java.classLoader,
        arrayOf(JarnRssData::class.java),
    ) { _, method, _ ->
        if (method.name == "getDateStyle") flowOf(JarnRssDateStyle()) else null
    } as JarnRssData

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    @Test
    fun `the clear focus target is the clear row`() {
        val clearFocus = FocusRequester()
        var clears = 0
        rule.setContent {
            JarnTheme {
                Column { JarnRssSettingsPanel(data, { clears++ }, clearFocus = clearFocus) }
            }
        }
        rule.runOnIdle { clearFocus.requestFocus() }
        rule.onNodeWithText(string(R.string.jarnfe_rss_settings_clear)).assertIsFocused()
        rule.onNodeWithText(string(R.string.jarnfe_rss_settings_day_first)).assertIsNotFocused()
        rule.onNodeWithText(string(R.string.jarnfe_rss_settings_clear)).performClick()
        assertEquals(1, clears)
    }
}
