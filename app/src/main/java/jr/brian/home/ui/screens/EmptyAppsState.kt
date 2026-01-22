package jr.brian.home.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun EmptyAppsState(
    onAddClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Companion.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.apps_tab_no_apps_title),
                color = Color.Companion.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Companion.Bold,
                textAlign = TextAlign.Companion.Center
            )

            Text(
                text = stringResource(R.string.apps_tab_no_apps_description),
                color = Color.Companion.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.Companion.height(16.dp))

            val cardGradient = Brush.Companion.linearGradient(
                colors = if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.9f),
                        ThemeSecondaryColor.copy(alpha = 0.9f)
                    )
                } else {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.4f),
                        ThemeSecondaryColor.copy(alpha = 0.3f)
                    )
                }
            )

            Box(
                modifier = Modifier.Companion
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        brush = cardGradient,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isFocused) 3.dp else 2.dp,
                        brush = if (isFocused) {
                            borderBrush(
                                isFocused = true,
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.8f),
                                    ThemeSecondaryColor.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.Companion.linearGradient(
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.6f),
                                    ThemeSecondaryColor.copy(alpha = 0.4f)
                                )
                            )
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .clickable { onAddClick() }
                    .focusable()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                contentAlignment = Alignment.Companion.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Companion.White,
                        modifier = Modifier.Companion.size(24.dp)
                    )
                    Spacer(modifier = Modifier.Companion.size(12.dp))
                    Text(
                        text = stringResource(R.string.apps_tab_add_button),
                        color = Color.Companion.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Companion.Bold
                    )
                }
            }
        }
    }
}