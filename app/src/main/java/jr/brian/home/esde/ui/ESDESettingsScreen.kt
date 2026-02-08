package jr.brian.home.esde.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.AnimationStyleSelector
import jr.brian.home.esde.ui.components.BackgroundColorSelector
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.esde.ui.components.LogoAlignmentSelector
import jr.brian.home.esde.ui.components.MarqueeSizeSetting
import jr.brian.home.esde.ui.components.MarqueeTabSettingsOption
import jr.brian.home.esde.ui.components.MusicVideoBehaviorSelector
import jr.brian.home.esde.ui.components.PathSetting
import jr.brian.home.esde.ui.components.ScreensaverBehaviorSelector
import jr.brian.home.esde.ui.components.SectionHeader
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.SystemImageTypeSelector
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.util.getPathFromUri
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.model.PageType
import jr.brian.home.model.Shortcut
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
    val pageCount = pageTypes.size
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_animation))
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_animation_duration),
                            value = prefsState.animationDuration.toFloat(),
                            valueRange = 100f..1000f,
                            steps = 8,
                            valueText = "${prefsState.animationDuration}ms",
                            onValueChange = { duration ->
                                preferencesManager.setAnimationDuration(duration.toInt())
                            }
                        )
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_animation_scale),
                            value = prefsState.animationScale,
                            valueRange = 0.5f..1.0f,
                            steps = 9,
                            valueText = "${(prefsState.animationScale * 100).toInt()}%",
                            onValueChange = { scale ->
                                preferencesManager.setAnimationScale(scale)
                            }
                        )
                    }

                    item {
                        AnimationStyleSelector(
                            selectedStyle = prefsState.animationStyle,
                            onStyleSelected = { style ->
                                preferencesManager.setAnimationStyle(style)
                            }
                        )
                    }

                    // App Drawer Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_app_drawer))
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_app_drawer_opacity),
                            value = prefsState.appDrawerOpacity.toFloat(),
                            valueRange = 0f..100f,
                            steps = 19,
                            valueText = "${prefsState.appDrawerOpacity}%",
                            onValueChange = { opacity ->
                                preferencesManager.setAppDrawerOpacity(opacity.toInt())
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_custom_paths))
                    }

                    item {
                        PathSetting(
                            title = stringResource(R.string.esde_settings_custom_system_images_path),
                            description = stringResource(R.string.esde_settings_custom_system_images_path_description),
                            currentPath = prefsState.customSystemImagesPath,
                            defaultText = stringResource(R.string.esde_settings_path_not_set),
                            onSelectPath = { systemImagesFolderPicker.launch(null) },
                            onClearPath = {
                                preferencesManager.setCustomSystemImagesPath(null)
                                viewModel.refreshSystemImage()
                            }
                        )
                    }

                    item {
                        PathSetting(
                            title = stringResource(R.string.esde_settings_custom_system_logos_path),
                            description = stringResource(R.string.esde_settings_custom_system_logos_path_description),
                            currentPath = prefsState.customSystemLogosPath,
                            defaultText = stringResource(R.string.esde_settings_path_not_set),
                            onSelectPath = { systemLogosFolderPicker.launch(null) },
                            onClearPath = {
                                preferencesManager.setCustomSystemLogosPath(null)
                                viewModel.refreshSystemImage()
                            }
                        )
                    }

                    item {
                        PathSetting(
                            title = stringResource(R.string.esde_settings_custom_media_path),
                            description = stringResource(R.string.esde_settings_custom_media_path_description),
                            currentPath = prefsState.customMediaPath,
                            defaultText = stringResource(R.string.esde_settings_path_not_set),
                            onSelectPath = { mediaFolderPicker.launch(null) },
                            onClearPath = {
                                preferencesManager.setCustomMediaPath(null)
                                viewModel.refreshSystemImage()
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_effects))
                    }

                    item {
                        BackgroundColorSelector(
                            selectedColor = Color(prefsState.backgroundColor),
                            onColorSelected = { color ->
                                preferencesManager.setBackgroundColor(color.toArgb())
                            }
                        )
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_blur_level),
                            value = prefsState.blurLevel.toFloat(),
                            valueRange = 0f..25f,
                            steps = 24,
                            valueText = if (prefsState.blurLevel == 0)
                                stringResource(R.string.esde_settings_off)
                            else
                                "${prefsState.blurLevel}",
                            onValueChange = { blur ->
                                preferencesManager.setBlurLevel(blur.toInt())
                            }
                        )
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_dimming_level),
                            value = prefsState.dimmingLevel.toFloat(),
                            valueRange = 0f..70f,
                            steps = 13,
                            valueText = "${prefsState.dimmingLevel}%",
                            onValueChange = { dimming ->
                                preferencesManager.setDimmingLevel(dimming.toInt())
                            }
                        )
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_exclude_effects_from_home),
                            description = stringResource(R.string.esde_settings_exclude_effects_from_home_description),
                            checked = prefsState.excludeEffectsFromHome,
                            onCheckedChange = { exclude ->
                                preferencesManager.setExcludeEffectsFromHome(exclude)
                            }
                        )
                    }

                    item {
                        GameImageTypeSelector(
                            selectedType = prefsState.gameImageType,
                            onTypeSelected = { type ->
                                preferencesManager.setGameImageType(type)
                            }
                        )
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_random_system_image),
                            description = stringResource(R.string.esde_settings_random_system_image_description),
                            checked = prefsState.randomSystemImage,
                            onCheckedChange = { random ->
                                preferencesManager.setRandomSystemImage(random)
                                viewModel.refreshSystemImage()
                            }
                        )
                    }

                    item {
                        SystemImageTypeSelector(
                            selectedType = prefsState.systemImageType,
                            onTypeSelected = { type ->
                                preferencesManager.setSystemImageType(type)
                                viewModel.refreshSystemImage()
                            }
                        )
                    }

                    // Marquee Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_marquee))
                    }

                    item {
                        LogoAlignmentSelector(
                            selectedAlignment = prefsState.logoAlignment,
                            onAlignmentSelected = { alignment ->
                                preferencesManager.setLogoAlignment(alignment)
                            }
                        )
                    }

                    item {
                        MarqueeSizeSetting(
                            title = stringResource(R.string.esde_settings_logo_size),
                            description = stringResource(R.string.esde_settings_logo_size_description),
                            width = prefsState.marqueeWidth,
                            height = prefsState.marqueeHeight,
                            widthLabel = stringResource(R.string.esde_settings_logo_width),
                            heightLabel = stringResource(R.string.esde_settings_logo_height),
                            resetLabel = stringResource(R.string.esde_settings_logo_size_reset),
                            onWidthChange = { width ->
                                preferencesManager.setMarqueeWidth(width)
                            },
                            onHeightChange = { height ->
                                preferencesManager.setMarqueeHeight(height)
                            },
                            onReset = {
                                preferencesManager.setMarqueeWidth(300)
                                preferencesManager.setMarqueeHeight(150)
                            }
                        )
                    }

                    item {
                        val currentShortcutLabel = when (prefsState.marqueePressShortcut) {
                            Shortcut.NONE -> stringResource(R.string.shortcut_none)
                            Shortcut.SETTINGS -> stringResource(R.string.shortcut_settings)
                            Shortcut.APP_SEARCH -> stringResource(R.string.shortcut_app_search)
                            Shortcut.POWERED_OFF -> stringResource(R.string.shortcut_powered_off)
                            Shortcut.QUICK_DELETE -> stringResource(R.string.shortcut_quick_delete)
                            Shortcut.CUSTOM_THEME -> stringResource(R.string.shortcut_custom_theme)
                            Shortcut.MONITOR -> stringResource(R.string.shortcut_monitor)
                            Shortcut.CONTROL_PAD -> stringResource(R.string.shortcut_control_pad)
                            Shortcut.VOLUME_CONTROLS -> stringResource(R.string.shortcut_volume_controls)
                            Shortcut.RECENT_APPS -> stringResource(R.string.shortcut_recent_apps)
                            Shortcut.APP -> stringResource(R.string.shortcut_app)
                        }

                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_marquee_press_shortcut),
                            description = stringResource(
                                R.string.esde_settings_marquee_press_shortcut_description
                            ) + "\n" + stringResource(
                                R.string.esde_settings_marquee_press_shortcut_choose
                            ) + ": $currentShortcutLabel",
                            checked = false,
                            showToggle = false,
                            onClick = onNavigateToMarqueePressShortcut
                        )
                    }

                    if (pageCount > 1) {
                        item {
                            Column {
                                Text(
                                    text = stringResource(R.string.esde_settings_marquee_tab_settings_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.esde_settings_marquee_tab_settings_description),
                                    color = Color.Gray.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (pageIndex in 0 until pageCount) {
                                        val isPageVisible =
                                            !prefsState.marqueeHiddenPages.contains(pageIndex)
                                        val isOverlayEnabled =
                                            prefsState.marqueeOverlayEnabledPages.contains(
                                                pageIndex
                                            )
                                        val isAppDrawerTab = pageTypes.getOrNull(pageIndex) == PageType.APP_DRAWER_TAB

                                        MarqueeTabSettingsOption(
                                            pageIndex = pageIndex,
                                            isVisible = isPageVisible,
                                            isOverlayEnabled = isOverlayEnabled,
                                            onVisibilityToggle = {
                                                preferencesManager.toggleMarqueePageVisibility(
                                                    pageIndex
                                                )
                                            },
                                            onOverlayToggle = {
                                                preferencesManager.toggleMarqueeOverlayPage(
                                                    pageIndex
                                                )
                                            },
                                            showOverlayOption = !isAppDrawerTab
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_music))
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_music_enabled),
                            description = stringResource(R.string.esde_settings_music_enabled_description),
                            checked = prefsState.musicEnabled,
                            onCheckedChange = { enabled ->
                                preferencesManager.setMusicEnabled(enabled)
                            }
                        )
                    }

                    if (prefsState.musicEnabled) {
                        item {
                            SliderSetting(
                                title = stringResource(R.string.esde_settings_music_volume),
                                value = prefsState.musicVolume.toFloat(),
                                valueRange = 0f..100f,
                                steps = 19,
                                valueText = "${prefsState.musicVolume}%",
                                onValueChange = { volume ->
                                    preferencesManager.setMusicVolume(volume.toInt())
                                    viewModel.musicController.setVolume(volume / 100f)
                                }
                            )
                        }

                        item {
                            PathSetting(
                                title = stringResource(R.string.esde_settings_music_path),
                                description = stringResource(R.string.esde_settings_music_path_description),
                                currentPath = prefsState.musicPath,
                                defaultText = stringResource(R.string.esde_settings_path_not_set),
                                onSelectPath = { musicFolderPicker.launch(null) },
                                onClearPath = {
                                    preferencesManager.setMusicPath(null)
                                }
                            )
                        }

                        item {
                            ToggleSetting(
                                title = stringResource(R.string.esde_settings_music_game),
                                description = stringResource(R.string.esde_settings_music_game_description),
                                checked = prefsState.musicGameEnabled,
                                onCheckedChange = { enabled ->
                                    preferencesManager.setMusicGameEnabled(enabled)
                                }
                            )
                        }

                        item {
                            ToggleSetting(
                                title = stringResource(R.string.esde_settings_music_screensaver),
                                description = stringResource(R.string.esde_settings_music_screensaver_description),
                                checked = prefsState.musicScreensaverEnabled,
                                onCheckedChange = { enabled ->
                                    preferencesManager.setMusicScreensaverEnabled(enabled)
                                }
                            )
                        }

                        item {
                            ToggleSetting(
                                title = stringResource(R.string.esde_settings_music_system),
                                description = stringResource(R.string.esde_settings_music_system_description),
                                checked = prefsState.musicSystemEnabled,
                                onCheckedChange = { enabled ->
                                    preferencesManager.setMusicSystemEnabled(enabled)
                                }
                            )
                        }

                        item {
                            MusicVideoBehaviorSelector(
                                selectedBehavior = prefsState.musicVideoBehavior,
                                onBehaviorSelected = { behavior ->
                                    preferencesManager.setMusicVideoBehavior(behavior)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_power))
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_persist_on_game_launch),
                            description = stringResource(R.string.esde_settings_persist_on_game_launch_description),
                            checked = prefsState.persistOnGameLaunch,
                            onCheckedChange = { persist ->
                                preferencesManager.setPersistOnGameLaunch(persist)
                                if (persist) {
                                    preferencesManager.setPowerEventsEnabled(false)
                                }
                            }
                        )
                    }

                    item {
                        AnimatedVisibility(prefsState.persistOnGameLaunch) {
                            SliderSetting(
                                title = stringResource(R.string.esde_settings_logo_brightness),
                                value = prefsState.logoBrightness.toFloat(),
                                valueRange = 0f..100f,
                                steps = 19,
                                valueText = "${prefsState.logoBrightness}%",
                                onValueChange = { brightness ->
                                    preferencesManager.setLogoBrightness(brightness.toInt())
                                }
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(prefsState.persistOnGameLaunch) {
                            SliderSetting(
                                title = stringResource(R.string.esde_settings_game_background_dimming),
                                value = prefsState.gameBackgroundDimming.toFloat(),
                                valueRange = 0f..70f,
                                steps = 13,
                                valueText = "${prefsState.gameBackgroundDimming}%",
                                onValueChange = { dimming ->
                                    preferencesManager.setGameBackgroundDimming(dimming.toInt())
                                }
                            )
                        }
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_power_events),
                            description = stringResource(R.string.esde_settings_power_events_description),
                            checked = prefsState.powerEventsEnabled,
                            onCheckedChange = { enabled ->
                                preferencesManager.setPowerEventsEnabled(enabled)
                                if (enabled) {
                                    preferencesManager.setPersistOnGameLaunch(false)
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_screensaver))
                    }

                    item {
                        ScreensaverBehaviorSelector(
                            selectedBehavior = prefsState.screensaverBehavior,
                            onBehaviorSelected = { behavior ->
                                preferencesManager.setScreensaverBehavior(behavior)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_setup))
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_run_setup_wizard),
                            description = stringResource(R.string.esde_settings_run_setup_wizard_description),
                            checked = false,
                            showToggle = false,
                            onClick = onRunSetupWizard
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_video))
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_hide_content_on_video),
                            description = stringResource(R.string.esde_settings_hide_content_on_video_description),
                            checked = prefsState.hideContentOnVideo,
                            onCheckedChange = { hide ->
                                preferencesManager.setHideContentOnVideo(hide)
                            }
                        )
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_video_audio),
                            description = stringResource(R.string.esde_settings_video_audio_description),
                            checked = prefsState.videoAudioEnabled,
                            onCheckedChange = { enabled ->
                                preferencesManager.setVideoAudioEnabled(enabled)
                            }
                        )
                    }

                    item {
                        SliderSetting(
                            title = stringResource(R.string.esde_settings_video_delay),
                            value = prefsState.videoDelaySeconds.toFloat(),
                            valueRange = 0f..10f,
                            steps = 9,
                            valueText = "${prefsState.videoDelaySeconds}s",
                            onValueChange = { delay ->
                                preferencesManager.setVideoDelaySeconds(delay.toInt())
                            }
                        )
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_video_enabled),
                            description = stringResource(R.string.esde_settings_video_enabled_description),
                            checked = prefsState.videoEnabled,
                            onCheckedChange = { enabled ->
                                preferencesManager.setVideoEnabled(enabled)
                            }
                        )
                    }
                }
            }
        }
    }
}


