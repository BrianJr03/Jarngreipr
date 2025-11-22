package jr.brian.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun OnScreenKeyboard(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit = {},
    onNavigateRight: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val letters = ('A'..'Z').toList()

    LaunchedEffect(Unit) {
        keyboardFocusRequesters[0]?.requestFocus()
    }

    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(AppCardDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = searchQuery.ifEmpty { "Search..." },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (searchQuery.isEmpty()) Color.Gray else Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KeyboardButton(
                        label = "←",
                        onClick = { onQueryChange(searchQuery.dropLast(1)) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        focusRequester =
                            remember(0) {
                                FocusRequester().also { keyboardFocusRequesters[0] = it }
                            },
                        onFocusChanged = { onFocusChanged(0) },
                        onNavigateRight = onNavigateRight,
                    )
                    KeyboardButton(
                        label = stringResource(R.string.keyboard_label_space),
                        onClick = { onQueryChange("$searchQuery ") },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        focusRequester =
                            remember(1) {
                                FocusRequester().also { keyboardFocusRequesters[1] = it }
                            },
                        onFocusChanged = { onFocusChanged(1) },
                        onNavigateRight = onNavigateRight,
                    )
                    KeyboardButton(
                        label = stringResource(R.string.keyboard_label_clear),
                        onClick = { onQueryChange("") },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        focusRequester =
                            remember(2) {
                                FocusRequester().also { keyboardFocusRequesters[2] = it }
                            },
                        onFocusChanged = { onFocusChanged(2) },
                        onNavigateRight = onNavigateRight,
                    )
                }
            }

            items(letters.chunked(3).size) { index ->
                val rowLetters = letters.chunked(3)[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowLetters.forEachIndexed { letterIndex, letter ->
                        val combinedIndex = index * 3 + letterIndex + 3
                        KeyboardButton(
                            label = letter.toString(),
                            onClick = { onQueryChange(searchQuery + letter) },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(56.dp),
                            focusRequester =
                                remember(combinedIndex) {
                                    FocusRequester().also {
                                        keyboardFocusRequesters[combinedIndex] = it
                                    }
                                },
                            onFocusChanged = { onFocusChanged(combinedIndex) },
                            onNavigateRight = onNavigateRight,
                        )
                    }
                    repeat(3 - rowLetters.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: () -> Unit = {},
    onNavigateRight: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
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
                    brush = cardGradient(isFocused),
                    shape = RoundedCornerShape(8.dp),
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    brush =
                        borderBrush(
                            isFocused = isFocused,
                            colors = listOf(
                                ThemeAccentColor,
                                ThemePrimaryColor,
                                ThemeSecondaryColor
                            ),
                        ),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable {
                    onClick()
                }
                .focusable()
                .handleRightNavigation(onNavigateRight),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        } else {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}