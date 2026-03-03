package jr.brian.home.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.extensions.clickWithHaptic

private val shape = RoundedCornerShape(16.dp)

@Composable
fun SettingsTextButton(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(subtleCardGradient(isFocused), shape)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                brush = borderBrush(isFocused),
                shape = shape
            )
            .clip(shape)
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = if (isFocused) 18.sp else 16.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
