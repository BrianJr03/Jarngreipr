package jr.brian.home.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun ThemeSelectorItem(
    focusRequester: FocusRequester? = null,
    isExpanded: Boolean = false,
    onExpandChanged: (Boolean) -> Unit = {},
    onNavigateToCustomTheme: () -> Unit = {}
) {
    val themeManager = LocalThemeManager.current
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }
    val allThemes = remember(themeManager.allThemes.size) { themeManager.allThemes }
    val selectedThemeFocusRequesters =
        remember(allThemes.size) { allThemes.associateWith { FocusRequester() } }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            val selectedTheme = themeManager.currentTheme
            selectedThemeFocusRequesters[selectedTheme]?.requestFocus()
        } else {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.8f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                    )
                } else {
                    listOf(
                        OledCardLightColor,
                        OledCardColor,
                    )
                },
        )

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester ?: mainCardFocusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                        }
                        .background(
                            brush = cardGradient,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            brush =
                                borderBrush(
                                    isFocused = isFocused,
                                    colors =
                                        listOf(
                                            ThemePrimaryColor.copy(alpha = 0.8f),
                                            ThemeSecondaryColor.copy(alpha = 0.6f),
                                        ),
                                ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onExpandChanged(true)
                        }
                        .focusable()
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.settings_palette_icon_description),
                        modifier =
                            Modifier
                                .size(32.dp)
                                .rotate(animatedRotation(isFocused)),
                        tint = Color.White,
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_color_theme_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_color_theme_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(allThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = themeManager.currentTheme.id == theme.id,
                        onClick = {
                            themeManager.setTheme(theme)
                            onExpandChanged(false)
                        },
                        onLongClick = if (theme.isCustom) {
                            { themeManager.deleteCustomTheme(theme) }
                        } else null,
                        focusRequester = selectedThemeFocusRequesters[theme],
                    )
                }

                item {
                    AddCustomThemeCard(
                        onClick = {
                            onNavigateToCustomTheme()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showDeleteIcon by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeManager = LocalThemeManager.current

    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    theme.primaryColor,
                    theme.secondaryColor,
                ),
        )

    val borderColor = when {
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier =
                Modifier
                    .width(120.dp)
                    .height(80.dp)
                    .scale(animatedFocusedScale(isFocused))
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        brush = gradient,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() }
                    .focusable()
                    .onFocusChanged {
                        isFocused = it.isFocused
                        showDeleteIcon = it.isFocused && theme.isCustom
                    },
            contentAlignment = Alignment.Center,
        ) {
            val themeName = if (theme.isCustom) {
                theme.customName ?: context.getString(R.string.theme_custom)
            } else {
                theme.nameResId?.let { context.getString(it) } ?: ""
            }
            val color = if (themeName == context.getString(R.string.theme_light_gray)) {
                Color.Black
            } else {
                Color.White
            }
            Text(
                text = themeName,
                color = color,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }

        if (theme.isCustom) {
            Box(
                modifier = Modifier
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(24.dp)
                    .background(
                        color = Color.Red,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        onClick = {
                            themeManager.deleteCustomTheme(theme)
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = context.getString(R.string.custom_theme_delete),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCustomThemeCard(
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .width(120.dp)
                .height(80.dp)
                .scale(animatedFocusedScale(isFocused))
                .background(
                    color = Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .focusable()
                .onFocusChanged {
                    isFocused = it.isFocused
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.custom_theme_add),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
