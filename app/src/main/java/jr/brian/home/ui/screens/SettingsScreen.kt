package jr.brian.home.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.settings.GridColumnSelectorItem
import jr.brian.home.ui.components.settings.HeaderVisibilityToggleItem
import jr.brian.home.ui.components.settings.OledModeToggleItem
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader
import jr.brian.home.ui.components.settings.ThemeSelectorItem
import jr.brian.home.ui.components.settings.ThorSettingsItem
import jr.brian.home.ui.components.settings.WallpaperSelectorItem
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.OverlayInfoUtil
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToCustomTheme: () -> Unit = {}
) {
    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .systemBarsPadding(),
        ) {
            Column {
                VersionInfo()
                SettingsContent(
                    allAppsUnfiltered = allAppsUnfiltered,
                    onNavigateToFAQ = onNavigateToFAQ,
                    onNavigateToCustomTheme = onNavigateToCustomTheme
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit = {},
    onNavigateToCustomTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val isThorDevice = remember {
        android.os.Build.MODEL == DeviceModel.THOR
    }

    LaunchedEffect(Unit) {
        delay(10)
        firstItemFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_appearance)
            )
        }

        item {
            ThemeSelectorItem(
                focusRequester = firstItemFocusRequester,
                isExpanded = expandedItem == "theme",
                onExpandChanged = { expandedItem = if (it) "theme" else null },
                onNavigateToCustomTheme = {
                    expandedItem = null
                    onNavigateToCustomTheme()
                }
            )
        }

        item {
            OledModeToggleItem(
                isExpanded = expandedItem == "oled"
            )
        }

        item {
            WallpaperSelectorItem(
                isExpanded = expandedItem == "wallpaper",
                onExpandChanged = { expandedItem = if (it) "wallpaper" else null }
            )
        }

        item {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_layout)
            )
        }

        item {
            HeaderVisibilityToggleItem(
                isExpanded = expandedItem == "header"
            )
        }

        item {
            GridColumnSelectorItem(
                isExpanded = expandedItem == "grid",
                onExpandChanged = { expandedItem = if (it) "grid" else null },
                totalAppsCount = allAppsUnfiltered.size
            )
        }

        if (isThorDevice) {
            item {
                ThorSettingsItem(
                    isExpanded = expandedItem == "thor",
                    onExpandChanged = { expandedItem = if (it) "thor" else null }
                )
            }
        }

        item {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_support)
            )
        }

        item {
            SettingItem(
                title = stringResource(id = R.string.settings_faq_title),
                description = stringResource(id = R.string.settings_faq_description),
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = {
                    expandedItem = null
                    onNavigateToFAQ()
                },
            )
        }

        item {
            val url = stringResource(R.string.settings_buy_me_coffee_url)
            SettingItem(
                title = stringResource(id = R.string.settings_buy_me_coffee_title),
                description = stringResource(id = R.string.settings_buy_me_coffee_description),
                icon = Icons.Default.Coffee,
                onClick = {
                    expandedItem = null
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        url.toUri()
                    )
                    context.startActivity(intent)
                },
            )
        }

        item {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_extras)
            )
        }

        item {
            val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
            InfoBox(
                label = stringResource(R.string.welcome_overlay_thor_fact_label),
                content = stringResource(randomMessage),
                isPrimary = true,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun VersionInfo() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1"
    } catch (_: Exception) {
        "0.1"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 32.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = stringResource(R.string.settings_version_label, versionName),
            color = ThemeAccentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
