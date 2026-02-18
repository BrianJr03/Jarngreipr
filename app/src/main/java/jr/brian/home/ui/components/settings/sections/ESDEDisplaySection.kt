package jr.brian.home.ui.components.settings.sections

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.preferences.WallpaperToggleTarget
import jr.brian.home.esde.ui.components.CollapsibleSection
import jr.brian.home.esde.ui.components.DeleteEmptyFoldersConfirmationDialog
import jr.brian.home.esde.ui.components.DeleteEmptyFoldersProgressDialog
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.sections.AnimationSectionContent
import jr.brian.home.esde.ui.sections.CustomPathsSectionContent
import jr.brian.home.esde.ui.sections.EffectsSectionContent
import jr.brian.home.esde.ui.sections.ExtrasSectionContent
import jr.brian.home.esde.ui.sections.MarqueeSectionContent
import jr.brian.home.esde.ui.sections.MusicSectionContent
import jr.brian.home.esde.ui.sections.PowerSectionContent
import jr.brian.home.esde.ui.sections.ScreensaverSectionContent
import jr.brian.home.esde.ui.sections.VideoSectionContent
import jr.brian.home.esde.util.getPathFromUri
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import kotlinx.coroutines.launch

@Composable
fun ESDESettingsContent(
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ESDEViewModel = hiltViewModel()
    val preferencesManager = LocalESDEPreferencesManager.current
    val pageTypeManager = LocalPageTypeManager.current
    val prefsState by preferencesManager.state.collectAsStateWithLifecycle()
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val cleanupFolderPaths by viewModel.cleanupManager.folderPaths.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
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

    val singleSystemImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            preferencesManager.setSingleSystemImagePath(it.toString())
            viewModel.refreshSystemImage()
        }
    }

    val singleSystemLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            preferencesManager.setSingleSystemLogoPath(it.toString())
            viewModel.refreshSystemImage()
        }
    }

    val singleGameImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            preferencesManager.setSingleGameImagePath(it.toString())
            viewModel.refreshSystemImage()
        }
    }

    val singleGameLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            preferencesManager.setSingleGameLogoPath(it.toString())
            viewModel.refreshSystemImage()
        }
    }

    val cleanupFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            scope.launch {
                viewModel.cleanupManager.addFolderPath(it.toString())
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteEmptyFoldersConfirmationDialog(
            onConfirm = {
                showDeleteConfirmation = false
                isDeleting = true
                viewModel.deleteEmptyESDEFolders()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
    
    if (isDeleting) {
        DeleteEmptyFoldersProgressDialog()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleSetting(
            title = stringResource(R.string.esde_settings_run_setup_wizard),
            description = stringResource(R.string.esde_settings_run_setup_wizard_description),
            checked = false,
            showToggle = false,
            onClick = onRunSetupWizard
        )

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_animation)) {
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
                },
                onMusicUseSystemSpecificChange = { useSystemSpecific ->
                    preferencesManager.setMusicUseSystemSpecific(useSystemSpecific)
                },
                onMusicLoopEnabledChange = { loopEnabled ->
                    preferencesManager.setMusicLoopEnabled(loopEnabled)
                }
            )
        }

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_custom_paths)) {
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
                onSelectSingleSystemImage = {
                    singleSystemImagePicker.launch(arrayOf("image/*", "video/mp4"))
                },
                onClearSingleSystemImage = {
                    preferencesManager.setSingleSystemImagePath(null)
                    viewModel.refreshSystemImage()
                },
                onSelectSingleSystemLogo = { 
                    singleSystemLogoPicker.launch(arrayOf("image/*"))
                },
                onClearSingleSystemLogo = {
                    preferencesManager.setSingleSystemLogoPath(null)
                    viewModel.refreshSystemImage()
                },
                onSelectSingleGameImage = { 
                    singleGameImagePicker.launch(arrayOf("image/*"))
                },
                onClearSingleGameImage = {
                    preferencesManager.setSingleGameImagePath(null)
                    viewModel.refreshSystemImage()
                },
                onSelectSingleGameLogo = { 
                    singleGameLogoPicker.launch(arrayOf("image/*"))
                },
                onClearSingleGameLogo = {
                    preferencesManager.setSingleGameLogoPath(null)
                    viewModel.refreshSystemImage()
                },
                onSelectMediaPath = { mediaFolderPicker.launch(null) },
                onClearMediaPath = {
                    preferencesManager.setCustomMediaPath(null)
                    viewModel.refreshSystemImage()
                }
            )
        }

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_marquee)) {
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
                onToggleDescriptionOverlayPage = { pageIndex ->
                    preferencesManager.toggleDescriptionOverlayPage(pageIndex)
                },
                onShowMarqueeForSystemChange = { show ->
                    preferencesManager.setShowLogoForSystem(show)
                },
                onShowMarqueeForGameChange = { show ->
                    preferencesManager.setShowLogoForGame(show)
                },
                onMarqueeMinWidthPercentChange = { percent ->
                    preferencesManager.setMarqueeMinWidthPercent(percent)
                },
                onOverlayMediaTypeChange = { type ->
                    preferencesManager.setOverlayMediaType(type)
                },
                onMarqueeOnlyModeChange = { enabled ->
                    preferencesManager.setLogoOnlyMode(enabled)
                }
            )
        }

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_power)) {
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

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_screensaver)) {
            ScreensaverSectionContent(
                prefsState = prefsState,
                onScreensaverBehaviorChange = { behavior ->
                    preferencesManager.setScreensaverBehavior(behavior)
                }
            )
        }

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_video)) {
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

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_effects)) {
            EffectsSectionContent(
                prefsState = prefsState,
                onBackgroundColorChange = { color ->
                    preferencesManager.setBackgroundColor(color)
                },
                onSystemBackgroundScaleModeChange = { mode ->
                    preferencesManager.setSystemBackgroundScaleMode(mode)
                },
                onGameBackgroundScaleModeChange = { mode ->
                    preferencesManager.setGameBackgroundScaleMode(mode)
                },
                onSystemBlurLevelChange = { blur ->
                    preferencesManager.setSystemBlurLevel(blur)
                },
                onGameBlurLevelChange = { blur ->
                    preferencesManager.setGameBlurLevel(blur)
                },
                onSystemBackgroundDimmingChange = { dimming ->
                    preferencesManager.setSystemBackgroundDimming(dimming)
                },
                onGameBackgroundDimmingChange = { dimming ->
                    preferencesManager.setGameBackgroundDimming(dimming)
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
                },
                onAndroidGamesBackgroundScaleChange = { scale ->
                    preferencesManager.setAndroidGamesBackgroundScale(scale)
                }
            )
        }

        CollapsibleSection(title = stringResource(R.string.esde_settings_section_extras)) {
            ExtrasSectionContent(
                hideUIForGameBrowsing = prefsState.hideUIForGameBrowsing,
                onHideUIForGameBrowsingChange = { hide ->
                    preferencesManager.setHideUIForGameBrowsing(hide)
                },
                folderPaths = cleanupFolderPaths.toList(),
                onAddFolder = {
                    cleanupFolderPicker.launch(null)
                },
                onRemoveFolder = { path ->
                    scope.launch {
                        viewModel.cleanupManager.removeFolderPath(path)
                    }
                },
                onDeleteEmptyFolders = {
                    showDeleteConfirmation = true
                }
            )
        }
    }
}

@Composable
fun ESDEDisplaySection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {}
) {
    CollapsibleSettingsSection(
        title = stringResource(R.string.esde_settings_title),
        icon = Icons.Default.Gamepad,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        ESDESettingsContent(
            onRunSetupWizard = onRunSetupWizard,
            onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut
        )
    }
}

@Composable
internal fun WallpaperToggleTargetSelector(
    selectedTarget: WallpaperToggleTarget,
    onTargetSelected: (WallpaperToggleTarget) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.esde_settings_wallpaper_toggle_target),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperToggleTarget.entries.forEach { target ->
                WallpaperToggleTargetChip(
                    target = target,
                    isSelected = target == selectedTarget,
                    onClick = { onTargetSelected(target) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun WallpaperToggleTargetChip(
    target: WallpaperToggleTarget,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(40.dp)
            .scale(animatedFocusedScale(isFocused))
            .background(
                color = when {
                    isSelected -> ThemePrimaryColor.copy(alpha = 0.7f)
                    isFocused -> ThemePrimaryColor.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected || isFocused) 1.dp else 0.dp,
                color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = target.displayName,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
