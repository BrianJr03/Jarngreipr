package jr.brian.home.esde.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.CollapsibleSection
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.sections.AnimationSectionContent
import jr.brian.home.esde.ui.sections.AppDrawerSectionContent
import jr.brian.home.esde.ui.sections.CustomPathsSectionContent
import jr.brian.home.esde.ui.sections.EffectsSectionContent
import jr.brian.home.esde.ui.sections.MarqueeSectionContent
import jr.brian.home.esde.ui.sections.MusicSectionContent
import jr.brian.home.esde.ui.sections.PowerSectionContent
import jr.brian.home.esde.ui.sections.ScreensaverSectionContent
import jr.brian.home.esde.ui.sections.VideoSectionContent
import jr.brian.home.esde.util.getPathFromUri
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalPageTypeManager

@Composable
fun ESDESettingsScreen(
    onNavigateBack: () -> Unit,
    onRunSetupWizard: () -> Unit,
    onNavigateToMarqueePressShortcut: () -> Unit = {},
    viewModel: ESDEViewModel = hiltViewModel()
) {
    val pageTypeManager = LocalPageTypeManager.current
    val preferencesManager = LocalESDEPreferencesManager.current
    val prefsState by preferencesManager.state.collectAsState()
    val pageTypes by pageTypeManager.pageTypes.collectAsState()
    BackHandler(onBack = onNavigateBack)

    val systemLogosFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                preferencesManager.setCustomSystemLogosPath(path)
                viewModel.refreshSystemImage()
            }
        }
    }

    val systemImagesFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                preferencesManager.setCustomSystemImagesPath(path)
                viewModel.refreshSystemImage()
            }
        }
    }

    val musicFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                preferencesManager.setMusicPath(path)
            }
        }
    }

    val mediaFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                preferencesManager.setCustomMediaPath(path)
                viewModel.refreshSystemImage()
            }
        }
    }

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onNavigateBack)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.esde_settings_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Run Setup Wizard (standalone, first item)
                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_run_setup_wizard),
                            description = stringResource(R.string.esde_settings_run_setup_wizard_description),
                            checked = false,
                            showToggle = false,
                            onClick = onRunSetupWizard
                        )
                    }

                    // Animation Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_animation)
                        ) {
                            AnimationSectionContent(
                                prefsState = prefsState,
                                onAnimationDurationChange = { duration ->
                                    preferencesManager.setAnimationDuration(duration)
                                },
                                onAnimationScaleChange = { scale ->
                                    preferencesManager.setAnimationScale(scale)
                                },
                                onAnimationStyleChange = { style ->
                                    preferencesManager.setAnimationStyle(style)
                                }
                            )
                        }
                    }

                    // App Drawer Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_app_drawer)
                        ) {
                            AppDrawerSectionContent(
                                prefsState = prefsState,
                                onAppDrawerOpacityChange = { opacity ->
                                    preferencesManager.setAppDrawerOpacity(opacity)
                                }
                            )
                        }
                    }

                    // Background Music Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_music)
                        ) {
                            MusicSectionContent(
                                prefsState = prefsState,
                                onMusicEnabledChange = { enabled ->
                                    preferencesManager.setMusicEnabled(enabled)
                                },
                                onMusicVolumeChange = { volume ->
                                    preferencesManager.setMusicVolume(volume)
                                    viewModel.musicController.setVolume(volume / 100f)
                                },
                                onSelectMusicPath = { musicFolderPicker.launch(null) },
                                onClearMusicPath = {
                                    preferencesManager.setMusicPath(null)
                                },
                                onMusicGameEnabledChange = { enabled ->
                                    preferencesManager.setMusicGameEnabled(enabled)
                                },
                                onMusicScreensaverEnabledChange = { enabled ->
                                    preferencesManager.setMusicScreensaverEnabled(enabled)
                                },
                                onMusicSystemEnabledChange = { enabled ->
                                    preferencesManager.setMusicSystemEnabled(enabled)
                                },
                                onMusicVideoBehaviorChange = { behavior ->
                                    preferencesManager.setMusicVideoBehavior(behavior)
                                }
                            )
                        }
                    }

                    // Custom Paths Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_custom_paths)
                        ) {
                            CustomPathsSectionContent(
                                prefsState = prefsState,
                                onSelectSystemImagesPath = { systemImagesFolderPicker.launch(null) },
                                onClearSystemImagesPath = {
                                    preferencesManager.setCustomSystemImagesPath(null)
                                    viewModel.refreshSystemImage()
                                },
                                onSelectSystemLogosPath = { systemLogosFolderPicker.launch(null) },
                                onClearSystemLogosPath = {
                                    preferencesManager.setCustomSystemLogosPath(null)
                                    viewModel.refreshSystemImage()
                                },
                                onSelectMediaPath = { mediaFolderPicker.launch(null) },
                                onClearMediaPath = {
                                    preferencesManager.setCustomMediaPath(null)
                                    viewModel.refreshSystemImage()
                                }
                            )
                        }
                    }

                    // Marquee Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_marquee)
                        ) {
                            MarqueeSectionContent(
                                prefsState = prefsState,
                                pageTypes = pageTypes,
                                onLogoAlignmentChange = { alignment ->
                                    preferencesManager.setLogoAlignment(alignment)
                                },
                                onMarqueeWidthChange = { width ->
                                    preferencesManager.setMarqueeWidth(width)
                                },
                                onMarqueeHeightChange = { height ->
                                    preferencesManager.setMarqueeHeight(height)
                                },
                                onMarqueeSizeReset = {
                                    preferencesManager.setMarqueeWidth(300)
                                    preferencesManager.setMarqueeHeight(150)
                                },
                                onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
                                onToggleMarqueePageVisibility = { pageIndex ->
                                    preferencesManager.toggleMarqueePageVisibility(pageIndex)
                                },
                                onToggleMarqueeOverlayPage = { pageIndex ->
                                    preferencesManager.toggleMarqueeOverlayPage(pageIndex)
                                }
                            )
                        }
                    }

                    // Power Events Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_power)
                        ) {
                            PowerSectionContent(
                                prefsState = prefsState,
                                onPersistOnGameLaunchChange = { persist ->
                                    preferencesManager.setPersistOnGameLaunch(persist)
                                    if (persist) {
                                        preferencesManager.setPowerEventsEnabled(false)
                                    }
                                },
                                onGameBackgroundDimmingChange = { dimming ->
                                    preferencesManager.setGameBackgroundDimming(dimming)
                                },
                                onPowerEventsEnabledChange = { enabled ->
                                    preferencesManager.setPowerEventsEnabled(enabled)
                                    if (enabled) {
                                        preferencesManager.setPersistOnGameLaunch(false)
                                    }
                                }
                            )
                        }
                    }

                    // Screensaver Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_screensaver)
                        ) {
                            ScreensaverSectionContent(
                                prefsState = prefsState,
                                onScreensaverBehaviorChange = { behavior ->
                                    preferencesManager.setScreensaverBehavior(behavior)
                                }
                            )
                        }
                    }

                    // Video Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_video)
                        ) {
                            VideoSectionContent(
                                prefsState = prefsState,
                                onVideoAudioEnabledChange = { enabled ->
                                    preferencesManager.setVideoAudioEnabled(enabled)
                                },
                                onVideoScaleModeChange = { mode ->
                                    preferencesManager.setVideoScaleMode(mode)
                                },
                                onVideoDelayChange = { delay ->
                                    preferencesManager.setVideoDelaySeconds(delay)
                                },
                                onVideoEnabledChange = { enabled ->
                                    preferencesManager.setVideoEnabled(enabled)
                                }
                            )
                        }
                    }

                    // Visual Effects Section
                    item {
                        CollapsibleSection(
                            title = stringResource(R.string.esde_settings_section_effects)
                        ) {
                            EffectsSectionContent(
                                prefsState = prefsState,
                                onBackgroundColorChange = { color ->
                                    preferencesManager.setBackgroundColor(color)
                                },
                                onBlurLevelChange = { blur ->
                                    preferencesManager.setBlurLevel(blur)
                                },
                                onDimmingLevelChange = { dimming ->
                                    preferencesManager.setDimmingLevel(dimming)
                                },
                                onExcludeEffectsFromHomeChange = { exclude ->
                                    preferencesManager.setExcludeEffectsFromHome(exclude)
                                },
                                onGameImageTypeChange = { type ->
                                    preferencesManager.setGameImageType(type)
                                },
                                onRandomSystemImageChange = { random ->
                                    preferencesManager.setRandomSystemImage(random)
                                    viewModel.refreshSystemImage()
                                },
                                onSystemImageTypeChange = { type ->
                                    preferencesManager.setSystemImageType(type)
                                    viewModel.refreshSystemImage()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
