package jr.brian.home.esde.ui.frontend.settings

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.data.setScreenScraperDevId
import jr.brian.home.esde.data.setScreenScraperDevPassword
import jr.brian.home.esde.data.setScreenScraperEnabled
import jr.brian.home.esde.data.setScreenScraperMediaTypes
import jr.brian.home.esde.data.setScreenScraperUserId
import jr.brian.home.esde.data.setScreenScraperUserPassword
import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapeState
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.frontend.settings.components.ScraperTextFieldRow
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.esde.viewmodels.ScraperViewModel
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun ScreenScraperSettingsScreen(
    onDismiss: () -> Unit
) {
    val prefs = LocalESDEPreferencesManager.current
    val state by prefs.state.collectAsStateWithLifecycle()
    val viewModel: ScraperViewModel = hiltViewModel()
    val romSearchViewModel: RomSearchViewModel = hiltViewModel()
    val scrapeState by viewModel.state.collectAsStateWithLifecycle()
    val testResult by viewModel.screenScraperTest.collectAsStateWithLifecycle()
    val testing by viewModel.testInFlight.collectAsStateWithLifecycle()

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> {
                        onDismiss(); true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.frontend_settings_screenscraper_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.frontend_settings_screenscraper_screen_summary),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )

            ToggleSetting(
                title = stringResource(R.string.scraper_enable_title),
                description = stringResource(R.string.scraper_enable_description),
                checked = state.screenScraperEnabled,
                onCheckedChange = prefs::setScreenScraperEnabled
            )

            SectionLabel(stringResource(R.string.scraper_credentials_section))

            ScraperTextFieldRow(
                label = stringResource(R.string.screenscraper_dev_id_label),
                value = state.screenScraperDevId,
                onValueChange = prefs::setScreenScraperDevId,
                placeholder = stringResource(R.string.screenscraper_dev_id_placeholder)
            )
            ScraperTextFieldRow(
                label = stringResource(R.string.screenscraper_dev_password_label),
                value = state.screenScraperDevPassword,
                onValueChange = prefs::setScreenScraperDevPassword,
                isPassword = true
            )
            ScraperTextFieldRow(
                label = stringResource(R.string.screenscraper_user_id_label),
                value = state.screenScraperUserId,
                onValueChange = prefs::setScreenScraperUserId,
                placeholder = stringResource(R.string.screenscraper_user_id_placeholder)
            )
            ScraperTextFieldRow(
                label = stringResource(R.string.screenscraper_user_password_label),
                value = state.screenScraperUserPassword,
                onValueChange = prefs::setScreenScraperUserPassword,
                isPassword = true
            )

            SectionLabel(stringResource(R.string.scraper_media_types_section))
            ScraperMediaKind.entries.filter { it.supportedByScreenScraper }.forEach { kind ->
                val enabled = kind.name in state.screenScraperMediaTypes
                ToggleSetting(
                    title = kind.displayName,
                    description = "",
                    checked = enabled,
                    onCheckedChange = { checked ->
                        val next = if (checked) state.screenScraperMediaTypes + kind.name
                        else state.screenScraperMediaTypes - kind.name
                        prefs.setScreenScraperMediaTypes(next)
                    }
                )
            }

            SectionLabel(stringResource(R.string.scraper_actions_section))
            ScraperActionsRow(
                testing = testing,
                testLabel = stringResource(R.string.scraper_test_credentials),
                scrapeLabel = stringResource(R.string.scraper_scrape_now),
                scrapeEnabled = state.screenScraperEnabled &&
                    scrapeState !is ScrapeState.Running,
                onTest = viewModel::testScreenScraper,
                onScrape = { viewModel.startScrape { romSearchViewModel.refreshGames() } }
            )
            testResult?.let { result ->
                ScraperResultCard(ok = result.ok, message = result.message)
            }

            ScraperProgressCard(
                state = scrapeState,
                onCancel = viewModel::cancel,
                onDismiss = viewModel::dismissResult
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        color = ThemePrimaryColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun ScraperActionsRow(
    testing: Boolean,
    testLabel: String,
    scrapeLabel: String,
    scrapeEnabled: Boolean,
    onTest: () -> Unit,
    onScrape: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = !testing,
            modifier = Modifier.weight(1f)
        ) {
            if (testing) {
                CircularProgressIndicator(
                    color = ThemePrimaryColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.width(14.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(testLabel)
        }
        Button(
            onClick = onScrape,
            enabled = scrapeEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimaryColor,
                contentColor = Color.Black
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(scrapeLabel)
        }
    }
}

@Composable
internal fun ScraperResultCard(ok: Boolean, message: String) {
    val bg = if (ok) ThemePrimaryColor.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = message,
            color = if (ok) ThemePrimaryColor else Color.Red.copy(alpha = 0.95f),
            fontSize = 13.sp
        )
    }
}

@Composable
internal fun ScraperProgressCard(
    state: ScrapeState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    when (state) {
        is ScrapeState.Idle -> Unit
        is ScrapeState.Running -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.scraper_progress_running,
                        state.done,
                        state.total,
                        state.downloads,
                        state.errors
                    ),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                state.currentTitle?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                val progress = if (state.total > 0) state.done.toFloat() / state.total else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    color = ThemePrimaryColor,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.scraper_cancel))
                }
            }
        }
        is ScrapeState.Finished -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemePrimaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.scraper_progress_finished,
                        state.processed,
                        state.downloads,
                        state.errors
                    ),
                    color = ThemePrimaryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.scraper_dismiss))
                }
            }
        }
        is ScrapeState.Cancelled -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.scraper_cancelled),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.scraper_dismiss))
                }
            }
        }
    }
}
