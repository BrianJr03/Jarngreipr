package jr.brian.home.ui.screens.jingles

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.R
import jr.brian.home.model.jingles.EntryResult
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.viewmodels.JingleEntryViewModel

private enum class ActiveJingleField(@StringRes val labelRes: Int) {
    PACK_NAME(R.string.jingles_add_pack_name_label),
    GAME_NAME(R.string.jingles_add_game_label),
    PLATFORM(R.string.jingles_add_platform_label),
    FILE_NAME(R.string.jingles_add_jingle_name_label),
}

@Composable
fun AddJingleScreen(
    localFolderUri: Uri,
    createPack: Boolean = false,
    existingPackPath: String? = null,
    existingPackName: String? = null,
    onDismiss: () -> Unit = {},
    onSuccess: () -> Unit = {},
    viewModel: JingleEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeField by remember { mutableStateOf<ActiveJingleField?>(null) }
    var tempText by remember { mutableStateOf("") }
    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    LaunchedEffect(existingPackPath, existingPackName) {
        if (existingPackPath != null && existingPackName != null) {
            viewModel.onExistingPackSelected(existingPackName, existingPackPath)
        }
    }

    BackHandler {
        if (activeField != null) activeField = null else onDismiss()
    }

    val mp3Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onMp3Selected(it) }
    }

    val successMsg = stringResource(R.string.jingles_add_success)

    LaunchedEffect(uiState.result) {
        when (val r = uiState.result) {
            is EntryResult.Success -> {
                snackbarHostState.showSnackbar(successMsg)
                viewModel.resetForm()
                onSuccess()
            }
            is EntryResult.Failure -> {
                snackbarHostState.showSnackbar(r.message)
                viewModel.clearResult()
            }
            null -> {}
        }
    }

    fun openField(field: ActiveJingleField, currentValue: String) {
        tempText = currentValue.lowercase()
        activeField = field
    }

    fun commitField() {
        when (activeField) {
            ActiveJingleField.PACK_NAME -> viewModel.onPackNameChange(tempText)
            ActiveJingleField.GAME_NAME -> viewModel.onGameNameChange(tempText)
            ActiveJingleField.PLATFORM -> viewModel.onPlatformChange(tempText)
            ActiveJingleField.FILE_NAME -> viewModel.onFileNameChange(tempText)
            null -> {}
        }
        activeField = null
    }

    Scaffold(
        containerColor = OledBackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBackgroundColor)
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            AnimatedVisibility(
                visible = activeField == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    ScreenHeader(onBackClick = onDismiss)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.jingles_add_screen_title),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(4.dp))

                        if (createPack) {
                            val existingPackOptions by viewModel.localPackOptions.collectAsStateWithLifecycle()
                            var showPackDropdown by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FieldBox(
                                        label = stringResource(R.string.jingles_add_pack_name_label),
                                        value = uiState.packName,
                                        placeholder = stringResource(R.string.jingles_add_pack_name_placeholder),
                                        enabled = !uiState.isProcessing,
                                        onClick = { openField(ActiveJingleField.PACK_NAME, uiState.packName) }
                                    )
                                }

                                if (existingPackOptions.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(
                                                    subtleCardGradient(showPackDropdown),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    width = if (showPackDropdown) 2.dp else 1.dp,
                                                    brush = borderBrush(showPackDropdown),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickWithHaptic(LocalHapticFeedback.current) {
                                                    showPackDropdown = true
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.jingles_add_pack_select_existing),
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showPackDropdown,
                                            onDismissRequest = { showPackDropdown = false },
                                            containerColor = OledCardColor
                                        ) {
                                            existingPackOptions.forEach { (name, path) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = name,
                                                            color = Color.White,
                                                            fontSize = 14.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.onExistingPackSelected(name, path)
                                                        showPackDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.existingPackPath != null) {
                                Text(
                                    text = stringResource(R.string.jingles_add_pack_updating),
                                    fontSize = 12.sp,
                                    color = ThemePrimaryColor,
                                    modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                )
                            }
                        }

                        FieldBox(
                            label = stringResource(R.string.jingles_add_game_label),
                            value = uiState.gameName,
                            placeholder = stringResource(R.string.jingles_add_game_placeholder),
                            enabled = !uiState.isProcessing,
                            onClick = { openField(ActiveJingleField.GAME_NAME, uiState.gameName) }
                        )

                        FieldBox(
                            label = stringResource(R.string.jingles_add_platform_label),
                            value = uiState.platform,
                            placeholder = stringResource(R.string.jingles_add_platform_placeholder),
                            supportingText = stringResource(R.string.jingles_add_platform_supporting),
                            enabled = !uiState.isProcessing,
                            onClick = { openField(ActiveJingleField.PLATFORM, uiState.platform) }
                        )

                        FieldBox(
                            label = stringResource(R.string.jingles_add_jingle_name_label),
                            value = uiState.fileName,
                            placeholder = stringResource(R.string.jingles_add_jingle_name_placeholder),
                            supportingText = stringResource(R.string.jingles_add_jingle_name_supporting),
                            enabled = !uiState.isProcessing,
                            onClick = { openField(ActiveJingleField.FILE_NAME, uiState.fileName) }
                        )

                        Spacer(Modifier.height(4.dp))

                        PickAudioButton(
                            enabled = !uiState.isProcessing,
                            onClick = { mp3Launcher.launch("audio/*") }
                        )

                        AnimatedVisibility(
                            visible = uiState.selectedMp3DisplayName.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(subtleCardGradient(true))
                                    .border(1.dp, borderBrush(true), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ThemePrimaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.selectedMp3DisplayName,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.gameName.isNotBlank()
                                    || uiState.platform.isNotBlank()
                                    || uiState.fileName.isNotBlank()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(subtleCardGradient(false))
                                    .border(1.dp, borderBrush(false), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.jingles_add_preview_label),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.45f),
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                val platform = uiState.platform.trim().lowercase()
                                val hasPlatform = platform.isNotBlank()
                                val fileName = uiState.fileName.trim().let {
                                    if (it.contains('.')) it
                                    else {
                                        val ext = uiState.selectedMp3DisplayName
                                            .substringAfterLast('.', "mp3").lowercase()
                                        "$it.$ext"
                                    }
                                }
                                val filePath = if (hasPlatform) "jingles/$platform/$fileName" else "jingles/$fileName"
                                Text(
                                    text = buildString {
                                        if (hasPlatform) {
                                            appendLine("\"$platform\": [")
                                            appendLine("  {")
                                            appendLine("    \"game\": \"${uiState.gameName.trim()}\",")
                                            appendLine("    \"file\": \"$filePath\"")
                                            appendLine("  }")
                                            append("]")
                                        } else {
                                            appendLine("{")
                                            appendLine("  \"game\": \"${uiState.gameName.trim()}\",")
                                            append("  \"file\": \"$filePath\"")
                                            appendLine()
                                            append("}")
                                        }
                                    },
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        SubmitButton(
                            isProcessing = uiState.isProcessing,
                            onClick = { viewModel.addJingle(localFolderUri, createPack) }
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            AnimatedVisibility(
                visible = activeField != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val field = activeField
                if (field != null) {
                    JinglesKeyboardOverlay(
                        fieldLabel = stringResource(field.labelRes),
                        tempText = tempText,
                        keyboardFocusRequesters = keyboardFocusRequesters,
                        onTextChange = { tempText = it.lowercase() },
                        onCancel = { activeField = null },
                        onDone = { commitField() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun JinglesKeyboardOverlay(
    fieldLabel: String,
    tempText: String,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackgroundColor)
    ) {
        Spacer(Modifier.weight(1f))

        // Current text display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = fieldLabel.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = tempText.ifBlank { stringResource(R.string.jingles_add_keyboard_hint) },
                fontSize = 20.sp,
                fontWeight = if (tempText.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (tempText.isBlank()) Color.Gray else Color.White,
                maxLines = 2,
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Cancel / Done row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickWithHaptic(haptic) { onCancel() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.jingles_add_cancel), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            listOf(ThemePrimaryColor.copy(alpha = 0.4f), ThemeSecondaryColor.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(1.dp, borderBrush(true), RoundedCornerShape(8.dp))
                    .clickWithHaptic(haptic) { onDone() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(stringResource(R.string.jingles_add_done), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        QwertyKeyboard(
            searchQuery = tempText,
            keyboardFocusRequesters = keyboardFocusRequesters,
            onQueryChange = onTextChange,
            showQueryText = false,
            showFlipLayoutButton = false,
        )
    }
}

@Composable
private fun FieldBox(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    val haptic = LocalHapticFeedback.current
    val hasValue = value.isNotBlank()
    val shape = RoundedCornerShape(12.dp)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(subtleCardGradient(hasValue), shape)
                .border(
                    width = 1.dp,
                    brush = if (hasValue) borderBrush(true) else borderBrush(false),
                    shape = shape
                )
                .then(if (enabled) Modifier.clickWithHaptic(haptic) { onClick() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(label, fontSize = 12.sp, color = if (hasValue) Color.White else Color.Gray)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value.ifBlank { placeholder },
                    fontSize = 16.sp,
                    color = if (!hasValue) Color.Gray.copy(alpha = 0.6f) else Color.White,
                )
            }
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PickAudioButton(enabled: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(subtleCardGradient(false), RoundedCornerShape(12.dp))
            .border(1.dp, borderBrush(false), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickWithHaptic(haptic) { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AudioFile,
            contentDescription = null,
            tint = if (enabled) ThemeSecondaryColor else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.jingles_add_pick_audio),
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SubmitButton(isProcessing: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    val submitShape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(ThemePrimaryColor.copy(alpha = 0.3f), ThemeSecondaryColor.copy(alpha = 0.2f))
                ),
                shape = submitShape
            )
            .then(
                if (isProcessing) {
                    Modifier.animatedGradientBorder(
                        colors = listOf(ThemePrimaryColor, ThemeSecondaryColor, ThemePrimaryColor),
                        shape = submitShape
                    )
                } else {
                    Modifier.border(1.dp, borderBrush(true), submitShape)
                }
            )
            .then(if (!isProcessing) Modifier.clickWithHaptic(haptic) { onClick() } else Modifier)
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = ThemePrimaryColor
            )
        } else {
            Text(
                text = stringResource(R.string.jingles_add_submit),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
