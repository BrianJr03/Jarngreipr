package jr.brian.home.ui.components

import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import jr.brian.home.R
import jr.brian.home.data.LocalJinglesManager
import jr.brian.home.esde.ui.video.VideoPresentationManager
import jr.brian.home.esde.viewmodels.ESDEViewModel
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalOledModeManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType

@OptIn(UnstableApi::class)
@Composable
fun QwertyKeyboard(
    searchQuery: String,
    modifier: Modifier = Modifier,
    showQueryText: Boolean = true,
    showFlipLayoutButton: Boolean = true,
    showSpecialCharRow: Boolean = true,
    showVolControl: Boolean = false,
    showSettings: Boolean = false,
    showController: Boolean = true,
    showAtKey: Boolean = true,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Int) -> Unit = {},
    onFlipLayout: () -> Unit = {},
    onSpecialCharToggle: () -> Unit = {},
    onReopenResults: (() -> Unit)? = null,
    onOpenRomSearchSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var isNumericMode by remember { mutableStateOf(false) }

    val wallpaperManager = LocalWallpaperManager.current
    val cursorTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by cursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val jinglesManager = LocalJinglesManager.current
    val esdeViewModel: ESDEViewModel = hiltViewModel(context as ComponentActivity)
    val isMuted by jinglesManager.isMuted.collectAsStateWithLifecycle()

    val qwertyRow1 = listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P')
    val qwertyRow2 = listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L')
    val qwertyRow3 = listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')
    val numbers = (1..9).toList() + 0

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showQueryText) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledCardColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        if (searchQuery.isEmpty()) {
                            append(stringResource(R.string.keyboard_label_search))
                        } else {
                            append(searchQuery)
                            withStyle(SpanStyle(color = ThemePrimaryColor.copy(alpha = cursorAlpha))) {
                                append("|")
                            }
                        }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (searchQuery.isEmpty()) Color.Gray else Color.White,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (showVolControl) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                ThemePrimaryColor.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                val newMuted = !isMuted
                                jinglesManager.setMuted(newMuted)
                                esdeViewModel.musicController.setMuted(newMuted)
                                VideoPresentationManager.setMuted(newMuted)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) ThemePrimaryColor.copy(alpha = 0.4f) else ThemePrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            ThemePrimaryColor.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSpecialCharToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "@",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                if (onReopenResults != null &&
                    wallpaperManager.getWallpaperType() == WallpaperType.ESDE
                    && showController
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                ThemePrimaryColor.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onReopenResults() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = stringResource(R.string.keyboard_label_reopen_results),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (showSettings) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                ThemePrimaryColor.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            ).clickable {
                               onOpenRomSearchSettings?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (showFlipLayoutButton) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                ThemePrimaryColor.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onFlipLayout() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = stringResource(R.string.keyboard_label_flip),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (isNumericMode) {
            QwertyNumericKeyboard(
                numbers = numbers,
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = onFocusChanged,
                onSwapMode = { isNumericMode = false }
            )
        } else {
            QwertyAlphabetKeyboard(
                qwertyRow1 = qwertyRow1,
                qwertyRow2 = qwertyRow2,
                qwertyRow3 = qwertyRow3,
                searchQuery = searchQuery,
                showSpecialCharRow = showSpecialCharRow,
                showAtKey = showAtKey,
                onQueryChange = onQueryChange,
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = onFocusChanged,
                onSwapMode = { isNumericMode = true }
            )
        }
    }
}

@Composable
private fun QwertyAlphabetKeyboard(
    qwertyRow1: List<Char>,
    qwertyRow2: List<Char>,
    qwertyRow3: List<Char>,
    searchQuery: String,
    showSpecialCharRow: Boolean = true,
    showAtKey: Boolean = false,
    onQueryChange: (String) -> Unit,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit,
    onSwapMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        qwertyRow1.forEachIndexed { index, letter ->
            QwertyKeyButton(
                label = letter.toString(),
                onClick = { onQueryChange(searchQuery + letter) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(index) {
                    FocusRequester().also { keyboardFocusRequesters[index] = it }
                },
                onFocusChanged = { onFocusChanged(index) },
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (!showAtKey) Spacer(modifier = Modifier.weight(0.5f))
        qwertyRow2.forEachIndexed { index, letter ->
            val combinedIndex = index + qwertyRow1.size
            QwertyKeyButton(
                label = letter.toString(),
                onClick = { onQueryChange(searchQuery + letter) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(combinedIndex) {
                    FocusRequester().also { keyboardFocusRequesters[combinedIndex] = it }
                },
                onFocusChanged = { onFocusChanged(combinedIndex) },
            )
        }
        if (showAtKey) {
            val atIndex = qwertyRow1.size + qwertyRow2.size
            QwertyKeyButton(
                label = "@",
                onClick = { onQueryChange("$searchQuery@") },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(atIndex) {
                    FocusRequester().also { keyboardFocusRequesters[atIndex] = it }
                },
                onFocusChanged = { onFocusChanged(atIndex) },
            )
        } else {
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val swapIndex = qwertyRow1.size + qwertyRow2.size + qwertyRow3.size
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_swap),
            onClick = onSwapMode,
            modifier = Modifier
                .weight(1.2f)
                .height(44.dp),
            icon = Icons.Default.SwapHoriz,
            focusRequester = remember(swapIndex) {
                FocusRequester().also { keyboardFocusRequesters[swapIndex] = it }
            },
            onFocusChanged = { onFocusChanged(swapIndex) },
        )

        qwertyRow3.forEachIndexed { index, letter ->
            val combinedIndex = index + qwertyRow1.size + qwertyRow2.size
            QwertyKeyButton(
                label = letter.toString(),
                onClick = { onQueryChange(searchQuery + letter) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(combinedIndex) {
                    FocusRequester().also { keyboardFocusRequesters[combinedIndex] = it }
                },
                onFocusChanged = { onFocusChanged(combinedIndex) },
            )
        }

        val backspaceIndex = swapIndex + 1
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_arrow_left),
            onClick = { onQueryChange(searchQuery.dropLast(1)) },
            modifier = Modifier
                .weight(1.2f)
                .height(44.dp),
            icon = Icons.AutoMirrored.Filled.Backspace,
            focusRequester = remember(backspaceIndex) {
                FocusRequester().also { keyboardFocusRequesters[backspaceIndex] = it }
            },
            onFocusChanged = { onFocusChanged(backspaceIndex) },
        )
    }

    AnimatedVisibility(showSpecialCharRow) {
        SpecialCharsRow(
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            keyboardFocusRequesters = keyboardFocusRequesters,
            onFocusChanged = onFocusChanged,
            indexOffset = 50,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val spaceIndex = qwertyRow1.size + qwertyRow2.size + qwertyRow3.size + 2
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_space),
            onClick = { onQueryChange("$searchQuery ") },
            modifier = Modifier
                .weight(3f)
                .height(44.dp),
            focusRequester = remember(spaceIndex) {
                FocusRequester().also { keyboardFocusRequesters[spaceIndex] = it }
            },
            onFocusChanged = { onFocusChanged(spaceIndex) },
        )

        val clearIndex = spaceIndex + 1
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_clear),
            onClick = { onQueryChange("") },
            modifier = Modifier
                .weight(1.5f)
                .height(44.dp),
            focusRequester = remember(clearIndex) {
                FocusRequester().also { keyboardFocusRequesters[clearIndex] = it }
            },
            onFocusChanged = { onFocusChanged(clearIndex) },
        )
    }
}

@Composable
private fun QwertyNumericKeyboard(
    numbers: List<Int>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit,
    onSwapMode: () -> Unit,
) {
    // Row 1: 1-5
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        numbers.take(5).forEachIndexed { index, number ->
            QwertyKeyButton(
                label = number.toString(),
                onClick = { onQueryChange(searchQuery + number) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(index) {
                    FocusRequester().also { keyboardFocusRequesters[index] = it }
                },
                onFocusChanged = { onFocusChanged(index) },
            )
        }
    }

    // Row 2: 6-0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        numbers.drop(5).forEachIndexed { index, number ->
            val combinedIndex = index + 5
            QwertyKeyButton(
                label = number.toString(),
                onClick = { onQueryChange(searchQuery + number) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                focusRequester = remember(combinedIndex) {
                    FocusRequester().also { keyboardFocusRequesters[combinedIndex] = it }
                },
                onFocusChanged = { onFocusChanged(combinedIndex) },
            )
        }
    }

    SpecialCharsRow(
        searchQuery = searchQuery,
        onQueryChange = onQueryChange,
        keyboardFocusRequesters = keyboardFocusRequesters,
        onFocusChanged = onFocusChanged,
        indexOffset = 20,
    )

    // Row 3: Swap, Space, Backspace, Clear
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val swapIndex = 10
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_swap),
            onClick = onSwapMode,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            icon = Icons.Default.SwapHoriz,
            focusRequester = remember(swapIndex) {
                FocusRequester().also { keyboardFocusRequesters[swapIndex] = it }
            },
            onFocusChanged = { onFocusChanged(swapIndex) },
        )

        val spaceIndex = 11
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_space),
            onClick = { onQueryChange("$searchQuery ") },
            modifier = Modifier
                .weight(2f)
                .height(44.dp),
            focusRequester = remember(spaceIndex) {
                FocusRequester().also { keyboardFocusRequesters[spaceIndex] = it }
            },
            onFocusChanged = { onFocusChanged(spaceIndex) },
        )

        val backspaceIndex = 12
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_arrow_left),
            onClick = { onQueryChange(searchQuery.dropLast(1)) },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            icon = Icons.AutoMirrored.Filled.Backspace,
            focusRequester = remember(backspaceIndex) {
                FocusRequester().also { keyboardFocusRequesters[backspaceIndex] = it }
            },
            onFocusChanged = { onFocusChanged(backspaceIndex) },
        )

        val clearIndex = 13
        QwertyKeyButton(
            label = stringResource(R.string.keyboard_label_clear),
            onClick = { onQueryChange("") },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            focusRequester = remember(clearIndex) {
                FocusRequester().also { keyboardFocusRequesters[clearIndex] = it }
            },
            onFocusChanged = { onFocusChanged(clearIndex) },
        )
    }
}

@Composable
private fun SpecialCharsRow(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit,
    indexOffset: Int,
) {
    val chars = listOf('-', '_', '.', '/', '(', ')', '\'', ':', ',', '!', '?', '&')
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        chars.forEachIndexed { i, char ->
            val idx = indexOffset + i
            QwertyKeyButton(
                label = char.toString(),
                onClick = { onQueryChange(searchQuery + char) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                focusRequester = remember(idx) {
                    FocusRequester().also { keyboardFocusRequesters[idx] = it }
                },
                onFocusChanged = { onFocusChanged(idx) },
            )
        }
    }
}

@Composable
private fun QwertyKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val oledManager = LocalOledModeManager.current
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .scale(pressScale)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .onFocusChanged {
                if (it.isFocused && !isFocused) {
                    onFocusChanged()
                }
                isFocused = it.isFocused
            }
            .background(
                brush = cardGradient(isFocused, isPressed = isPressed, ignoreOled = oledManager.isKeyboardOledExempt),
                shape = RoundedCornerShape(6.dp),
            )
            .border(
                width = if (isFocused || isPressed) 2.dp else 0.dp,
                brush = borderBrush(
                    isFocused = isFocused || isPressed,
                    colors = listOf(
                        ThemeAccentColor,
                        ThemePrimaryColor,
                        ThemeSecondaryColor
                    ),
                ),
                shape = RoundedCornerShape(6.dp),
            )
            .pressWithHaptic(
                onClick, label,
                haptic = haptic,
                onPressChange = { isPressed = it },
                onClick = onClick
            )
            .focusable()
            .handleUpNavigation(),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

private fun Modifier.handleUpNavigation(): Modifier {
    return this.then(
        Modifier.onFocusChanged { /* handled in parent */ }
    )
}
