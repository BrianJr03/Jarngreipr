package jr.brian.home.esde.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.ThorSettingToggleButton
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.themeSecondaryColor

@Composable
internal fun RomSearchSettingsScreen(onBack: () -> Unit) {
    val prefsManager = LocalESDEPreferencesManager.current
    val state by prefsManager.state.collectAsStateWithLifecycle()
    var showInfoBox by rememberSaveable { mutableStateOf(false) }
    var animationExpanded by rememberSaveable { mutableStateOf(false) }
    var cardMediaExpanded by rememberSaveable { mutableStateOf(false) }
    var displayExpanded by rememberSaveable { mutableStateOf(false) }
    val secondaryColor = themeSecondaryColor()

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.rom_search_settings_back),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = stringResource(R.string.rom_search_settings_title),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showInfoBox = !showInfoBox }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.rom_search_settings_back),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(showInfoBox) {
                val commandsInfo = romSearchCommands()
                    .joinToString("\n") { (cmd, desc) -> "• $cmd — $desc" }
                InfoBox(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    label = stringResource(R.string.rom_search_commands),
                    content = commandsInfo,
                    highlightedTerms = listOf(
                        stringResource(R.string.rom_search_command_android),
                        stringResource(R.string.rom_search_command_hidden),
                        stringResource(R.string.rom_search_command_platform),
                        stringResource(R.string.rom_search_command_partial),
                        stringResource(R.string.rom_search_command_name),
                    ),
                    contentTextColor = ThemeSecondaryColor
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animation section
                item {
                    CollapsibleSettingsSection(
                        title = "Animation",
                        icon = Icons.Default.Animation,
                        isExpanded = animationExpanded,
                        onToggle = { animationExpanded = !animationExpanded }
                    ) {
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_focus_animation_title),
                            description = stringResource(R.string.rom_search_settings_focus_animation_description),
                            checked = state.romSearchDiscSpin,
                            onCheckedChange = { prefsManager.setRomSearchDiscSpin(it) }
                        )
                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Focus Animation Delay",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${state.romSearchFocusAnimationDelayMs}ms",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Delay before animations trigger while scrolling",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Slider(
                                value = state.romSearchFocusAnimationDelayMs.toFloat(),
                                onValueChange = { prefsManager.setRomSearchFocusAnimationDelayMs(it.toInt()) },
                                valueRange = 0f..1000f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Card Media section
                item {
                    CollapsibleSettingsSection(
                        title = stringResource(R.string.rom_search_settings_section_card_media),
                        icon = Icons.Default.Image,
                        isExpanded = cardMediaExpanded,
                        onToggle = { cardMediaExpanded = !cardMediaExpanded }
                    ) {
                        Text(
                            text = stringResource(R.string.rom_search_settings_card_media_description),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        RomSearchCardMediaType.entries.forEach { type ->
                            ThorSettingToggleButton(
                                text = type.displayName,
                                isChecked = state.romSearchCardMediaType == type,
                                onClick = { prefsManager.setRomSearchCardMediaType(type) }
                            )
                        }
                    }
                }

                // Display section
                item {
                    CollapsibleSettingsSection(
                        title = stringResource(R.string.rom_search_settings_section_display),
                        icon = Icons.Default.Tune,
                        isExpanded = displayExpanded,
                        onToggle = { displayExpanded = !displayExpanded }
                    ) {
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_black_bg_title),
                            description = stringResource(R.string.rom_search_settings_black_bg_description),
                            checked = state.romSearchBlackBackground,
                            onCheckedChange = { prefsManager.setRomSearchBlackBackground(it) }
                        )
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_wallpaper_title),
                            description = stringResource(R.string.rom_search_settings_wallpaper_description),
                            checked = state.romSearchUseWallpaper,
                            onCheckedChange = { prefsManager.setRomSearchUseWallpaper(it) }
                        )
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_hide_no_image_title),
                            description = stringResource(R.string.rom_search_settings_hide_no_image_description),
                            checked = state.romSearchHideNoImage,
                            onCheckedChange = { prefsManager.setRomSearchHideNoImage(it) }
                        )
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_hide_no_metadata_title),
                            description = stringResource(R.string.rom_search_settings_hide_no_metadata_description),
                            checked = state.romSearchHideNoMetadata,
                            onCheckedChange = { prefsManager.setRomSearchHideNoMetadata(it) }
                        )
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_platform_auto_filter_title),
                            description = stringResource(R.string.rom_search_settings_platform_auto_filter_description),
                            checked = state.romSearchPlatformAutoFilter,
                            onCheckedChange = { prefsManager.setRomSearchPlatformAutoFilter(it) }
                        )
                        val androidAppsDesc = stringResource(R.string.rom_search_settings_show_android_apps_description)
                        val androidAppsAnnotated = remember(androidAppsDesc) {
                            val term = "@android"
                            val idx = androidAppsDesc.indexOf(term, ignoreCase = true)
                            buildAnnotatedString {
                                append(androidAppsDesc)
                                if (idx >= 0) {
                                    addStyle(
                                        SpanStyle(color = secondaryColor),
                                        start = idx,
                                        end = idx + term.length
                                    )
                                }
                            }
                        }
                        ToggleSetting(
                            title = stringResource(R.string.rom_search_settings_show_android_apps_title),
                            description = androidAppsAnnotated,
                            checked = state.romSearchShowAllAndroidApps,
                            onCheckedChange = { prefsManager.setRomSearchShowAllAndroidApps(it) }
                        )
                    }
                }
            }
        }
    }
}

