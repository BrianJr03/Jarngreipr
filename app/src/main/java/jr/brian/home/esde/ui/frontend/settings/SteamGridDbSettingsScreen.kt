package jr.brian.home.esde.ui.frontend.settings

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import jr.brian.home.esde.data.setSteamGridDbApiKey
import jr.brian.home.esde.data.setSteamGridDbEnabled
import jr.brian.home.esde.data.setSteamGridDbMediaTypes
import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapeState
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.frontend.settings.components.ScraperTextFieldRow
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.esde.viewmodels.ScraperViewModel
import jr.brian.home.ui.theme.OledBackgroundColor

@Composable
fun SteamGridDbSettingsScreen(
    onDismiss: () -> Unit
) {
    val prefs = LocalESDEPreferencesManager.current
    val state by prefs.state.collectAsStateWithLifecycle()
    val viewModel: ScraperViewModel = hiltViewModel()
    val romSearchViewModel: RomSearchViewModel = hiltViewModel()
    val scrapeState by viewModel.state.collectAsStateWithLifecycle()
    val testResult by viewModel.steamGridDbTest.collectAsStateWithLifecycle()
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
                text = stringResource(R.string.frontend_settings_steamgriddb_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.frontend_settings_steamgriddb_screen_summary),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )

            ToggleSetting(
                title = stringResource(R.string.scraper_enable_title),
                description = stringResource(R.string.scraper_enable_description),
                checked = state.steamGridDbEnabled,
                onCheckedChange = prefs::setSteamGridDbEnabled
            )

            SectionLabel(stringResource(R.string.scraper_credentials_section))
            ScraperTextFieldRow(
                label = stringResource(R.string.steamgriddb_api_key_label),
                value = state.steamGridDbApiKey,
                onValueChange = prefs::setSteamGridDbApiKey,
                isPassword = true,
                placeholder = stringResource(R.string.steamgriddb_api_key_placeholder)
            )

            SectionLabel(stringResource(R.string.scraper_media_types_section))
            ScraperMediaKind.entries.filter { it.supportedBySteamGridDb }.forEach { kind ->
                val enabled = kind.name in state.steamGridDbMediaTypes
                ToggleSetting(
                    title = kind.displayName,
                    description = "",
                    checked = enabled,
                    onCheckedChange = { checked ->
                        val next = if (checked) state.steamGridDbMediaTypes + kind.name
                        else state.steamGridDbMediaTypes - kind.name
                        prefs.setSteamGridDbMediaTypes(next)
                    }
                )
            }

            SectionLabel(stringResource(R.string.scraper_actions_section))
            ScraperActionsRow(
                testing = testing,
                testLabel = stringResource(R.string.scraper_test_api_key),
                scrapeLabel = stringResource(R.string.scraper_scrape_now),
                scrapeEnabled = state.steamGridDbEnabled &&
                    scrapeState !is ScrapeState.Running,
                onTest = viewModel::testSteamGridDb,
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
