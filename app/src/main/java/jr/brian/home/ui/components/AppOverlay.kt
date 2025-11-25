package jr.brian.home.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as GraphicsColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.OverlayInfoUtil

@Composable
fun AppOverlay(
    onDismissOverlay: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
    val scrollState = rememberScrollState()

    val canScrollDown by remember {
        derivedStateOf {
            scrollState.value < scrollState.maxValue
        }
    }

    val canScrollUp by remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scroll_indicator")
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1275), // 15% faster
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphicsColor.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectTapGestures { }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppCardDark.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(40.dp)
                        .verticalScroll(scrollState)
                ) {
                Text(
                    text = stringResource(R.string.welcome_overlay_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = GraphicsColor.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.15f),
                                    ThemeSecondaryColor.copy(alpha = 0.15f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.5f),
                                    ThemeSecondaryColor.copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_overlay_thor_fact_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemePrimaryColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(randomMessage),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = GraphicsColor.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Start,
                            lineHeight = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AppCardLight.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = GraphicsColor.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_overlay_info_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GraphicsColor.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.welcome_overlay_extra_info),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = GraphicsColor.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Start,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onOpenSettings()
                            onDismissOverlay()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ThemePrimaryColor,
                            containerColor = GraphicsColor.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            ThemePrimaryColor.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.launch_settings_overlay_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GraphicsColor.White
                        )
                    }

                    Button(
                        onClick = onDismissOverlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemePrimaryColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_overlay_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GraphicsColor.White
                        )
                    }
                }
            }
            }

            IconButton(
                onClick = onDismissOverlay,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
                    .background(
                        color = ThemePrimaryColor.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = GraphicsColor.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (canScrollUp) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(start = 40.dp, top = 32.dp, end = 24.dp)
                    .alpha(arrowAlpha)
                    .background(
                        color = ThemePrimaryColor.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = GraphicsColor.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (canScrollDown) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 40.dp, bottom = 32.dp, end = 24.dp)
                    .alpha(arrowAlpha)
                    .background(
                        color = ThemePrimaryColor.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GraphicsColor.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
