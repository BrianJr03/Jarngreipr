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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager

@Composable
fun BackButtonShortcutItem(
    isExpanded: Boolean = false,
    focusRequester: FocusRequester? = null,
    onExpandChanged: (Boolean) -> Unit = {},
    onConfigureClick: () -> Unit = {}
) {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val isEnabled by powerSettingsManager.backButtonShortcutEnabled.collectAsStateWithLifecycle()
    var isFocused by remember { mutableStateOf(false) }
    val mainCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            mainCardFocusRequester.requestFocus()
        }
    }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.8f),
                ThemeSecondaryColor.copy(alpha = 0.8f)
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor
            )
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester ?: mainCardFocusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .background(
                        brush = cardGradient,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        brush = borderBrush(
                            isFocused = isFocused,
                            colors = listOf(
                                ThemePrimaryColor.copy(alpha = 0.8f),
                                ThemeSecondaryColor.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onExpandChanged(true)
                    }
                    .focusable()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(animatedRotation(isFocused)),
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_back_button_shortcut_title),
                            color = Color.White,
                            fontSize = if (isFocused) 18.sp else 16.sp,
                            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_back_button_shortcut_description),
                            color = if (isFocused) Color.White.copy(alpha = 0.9f) else Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThorSettingToggleButton(
                    text = stringResource(id = R.string.settings_back_button_shortcut_enabled),
                    isChecked = isEnabled,
                    onClick = {
                        val newEnabledState = !isEnabled
                        powerSettingsManager.setBackButtonShortcutEnabled(newEnabledState)
                        if (newEnabledState) {
                            onConfigureClick()
                        } else {
                            // Reset shortcut to NONE when disabled
                            powerSettingsManager.resetBackButtonShortcut()
                            onExpandChanged(false)
                        }
                    }
                )

                if (isEnabled) {
                    ThorSettingSelectionButton(
                        text = stringResource(id = R.string.settings_back_button_shortcut_choose),
                        currentValue = true,
                        targetValue = true,
                        onClick = {
                            onConfigureClick()
                        }
                    )
                }
            }
        }
    }
}
