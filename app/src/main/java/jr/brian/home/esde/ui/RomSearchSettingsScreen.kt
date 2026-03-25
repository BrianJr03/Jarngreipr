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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import jr.brian.home.ui.components.settings.ThorSettingToggleButton
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.themePrimaryColor
import jr.brian.home.ui.theme.themeSecondaryColor

@Composable
internal fun RomSearchSettingsScreen(onBack: () -> Unit) {
    val prefsManager = LocalESDEPreferencesManager.current
    val state by prefsManager.state.collectAsStateWithLifecycle()
    var showInfoBox by rememberSaveable { mutableStateOf(false) }
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
                InfoBox(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    label = stringResource(R.string.rom_search_commands),
                    content = stringResource(R.string.rom_search_commands_info),
                    highlightedTerms = listOf("@hidden", "@android", "@{system}"),
                    contentTextColor = ThemeSecondaryColor
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionLabel(stringResource(R.string.rom_search_settings_section_display))
                }

                item {
                    ToggleSetting(
                        title = stringResource(R.string.rom_search_settings_black_bg_title),
                        description = stringResource(R.string.rom_search_settings_black_bg_description),
                        checked = state.romSearchBlackBackground,
                        onCheckedChange = { prefsManager.setRomSearchBlackBackground(it) }
                    )
                }

                item {
                    ToggleSetting(
                        title = stringResource(R.string.rom_search_settings_wallpaper_title),
                        description = stringResource(R.string.rom_search_settings_wallpaper_description),
                        checked = state.romSearchUseWallpaper,
                        onCheckedChange = { prefsManager.setRomSearchUseWallpaper(it) }
                    )
                }

                item {
                    ToggleSetting(
                        title = stringResource(R.string.rom_search_settings_hide_no_image_title),
                        description = stringResource(R.string.rom_search_settings_hide_no_image_description),
                        checked = state.romSearchHideNoImage,
                        onCheckedChange = { prefsManager.setRomSearchHideNoImage(it) }
                    )
                }

                item {
                    ToggleSetting(
                        title = stringResource(R.string.rom_search_settings_hide_no_metadata_title),
                        description = stringResource(R.string.rom_search_settings_hide_no_metadata_description),
                        checked = state.romSearchHideNoMetadata,
                        onCheckedChange = { prefsManager.setRomSearchHideNoMetadata(it) }
                    )
                }

                item {
                    ToggleSetting(
                        title = stringResource(R.string.rom_search_settings_focus_animation_title),
                        description = stringResource(R.string.rom_search_settings_focus_animation_description),
                        checked = state.romSearchDiscSpin,
                        onCheckedChange = { prefsManager.setRomSearchDiscSpin(it) }
                    )
                }

                item {
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

                item {
                    SectionLabel(stringResource(R.string.rom_search_settings_section_card_media))
                }

                item {
                    Text(
                        text = stringResource(R.string.rom_search_settings_card_media_description),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(RomSearchCardMediaType.entries) { type ->
                    ThorSettingToggleButton(
                        text = type.displayName,
                        isChecked = state.romSearchCardMediaType == type,
                        onClick = { prefsManager.setRomSearchCardMediaType(type) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
    )
}
