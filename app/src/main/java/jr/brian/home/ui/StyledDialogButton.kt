package jr.brian.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun StyledDialogButton(
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    val backgroundGradient =
        Brush.linearGradient(
            colors =
                if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.9f),
                        ThemeSecondaryColor.copy(alpha = 0.8f),
                        ThemeAccentColor.copy(alpha = 0.7f),
                    )
                } else {
                    if (isPrimary) {
                        listOf(AppCardLight, AppCardDark)
                    } else {
                        listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.2f))
                    }
                },
        )

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                }.background(
                    brush = backgroundGradient,
                    shape = RoundedCornerShape(8.dp),
                ).border(
                    width = if (isFocused) 2.dp else 1.dp,
                    brush =
                        if (isFocused) {
                            borderBrush(
                                isFocused = true,
                                colors = listOf(ThemeAccentColor, ThemePrimaryColor, ThemeSecondaryColor),
                            )
                        } else {
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.Gray.copy(alpha = 0.5f),
                                        Color.Gray.copy(alpha = 0.5f),
                                    ),
                            )
                        },
                    shape = RoundedCornerShape(8.dp),
                ).clip(RoundedCornerShape(8.dp))
                .clickable {
                    onClick()
                }.focusable()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                if (isFocused) {
                    Color.White
                } else if (isPrimary) {
                    ThemeAccentColor
                } else {
                    Color.Gray
                },
            fontSize = if (isFocused) 16.sp else 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}