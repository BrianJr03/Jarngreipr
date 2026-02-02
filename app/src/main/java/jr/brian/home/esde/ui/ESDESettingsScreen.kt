package jr.brian.home.esde.ui

import androidx.activity.compose.BackHandler
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
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.AnimationStyleSelector
import jr.brian.home.esde.ui.components.BackgroundColorSelector
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.esde.ui.components.LogoAlignmentSelector
import jr.brian.home.esde.ui.components.SectionHeader
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.SystemImageTypeSelector
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor

@Composable
fun ESDESettingsScreen(
    onNavigateBack: () -> Unit,
    onRunSetupWizard: () -> Unit,
    viewModel: ESDEViewModel = hiltViewModel()
) {
    val preferencesManager = LocalESDEPreferencesManager.current
    val prefsState by preferencesManager.state.collectAsState()
    BackHandler(onBack = onNavigateBack)

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
                        SectionHeader(text = stringResource(R.string.esde_settings_section_animation))
                    }

                    item {
                        AnimationStyleSelector(
                            selectedStyle = prefsState.animationStyle,
                            onStyleSelected = { style ->
                                preferencesManager.setAnimationStyle(style)
                            }
                        )
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

                    if (prefsState.animationStyle in listOf(
                            AnimationStyle.ScaleFade,
                            AnimationStyle.Custom
                        )
                    ) {
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
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_effects))
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
                        GameImageTypeSelector(
                            selectedType = prefsState.gameImageType,
                            onTypeSelected = { type ->
                                preferencesManager.setGameImageType(type)
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
                            valueRange = 0f..100f,
                            steps = 19,
                            valueText = "${prefsState.dimmingLevel}%",
                            onValueChange = { dimming ->
                                preferencesManager.setDimmingLevel(dimming.toInt())
                            }
                        )
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
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_video))
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

                    if (prefsState.videoEnabled) {
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
                                title = stringResource(R.string.esde_settings_video_audio),
                                description = stringResource(R.string.esde_settings_video_audio_description),
                                checked = prefsState.videoAudioEnabled,
                                onCheckedChange = { enabled ->
                                    preferencesManager.setVideoAudioEnabled(enabled)
                                }
                            )
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
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(text = stringResource(R.string.esde_settings_section_logo))
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.esde_settings_show_system_logo),
                            description = stringResource(R.string.esde_settings_show_system_logo_description),
                            checked = prefsState.showSystemLogo,
                            onCheckedChange = { show ->
                                preferencesManager.setShowSystemLogo(show)
                            }
                        )
                    }

                    if (prefsState.showSystemLogo) {
                        item {
                            LogoAlignmentSelector(
                                selectedAlignment = prefsState.logoAlignment,
                                onAlignmentSelected = { alignment ->
                                    preferencesManager.setLogoAlignment(alignment)
                                }
                            )
                        }
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
                }
            }
        }
    }
}
