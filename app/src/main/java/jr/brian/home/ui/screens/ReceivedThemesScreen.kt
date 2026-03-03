package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.settings.PingAutoStartToggleItem
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.viewmodels.ReceivedThemesViewModel
import jr.brian.ping.PingUtil.hasPingPermissions

@Composable
fun ReceivedThemesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReceivedThemesViewModel = hiltViewModel()
) {
    val receivedThemes by viewModel.receivedThemes.collectAsStateWithLifecycle()
    val themeManager = LocalThemeManager.current
    val context = LocalContext.current

    // Initialize from auto-start preference so button reflects actual service state
    var isPinging by remember { mutableStateOf(themeManager.isPingAutoStart) }
    var showNameKeyboard by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
    var focusedKeyIndex by remember { mutableIntStateOf(0) }

    // Sync service state whenever auto-start preference changes from the toggle
    LaunchedEffect(themeManager.isPingAutoStart) {
        if (themeManager.isPingAutoStart && !isPinging) {
            if (context.hasPingPermissions()) {
                themeManager.shareAllCustomThemes()
                isPinging = true
            }
        } else if (!themeManager.isPingAutoStart && isPinging) {
            themeManager.stopSharing()
            isPinging = false
        }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onNavigateBack)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Received Themes",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap a theme to apply it",
                        color = ThemeSecondaryColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    IconButton(
                        onClick = {
                            if (context.hasPingPermissions()) {
                                if (isPinging) {
                                    themeManager.stopSharing()
                                } else {
                                    themeManager.shareAllCustomThemes()
                                }
                                isPinging = !isPinging
                            }
                        }
                    ) {
                        Text(
                            text = if (isPinging) "Stop" else "Ping",
                            color = if (isPinging) Color.Red.copy(alpha = 0.8f) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Name input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OledCardColor, RoundedCornerShape(8.dp))
                            .clickable { showNameKeyboard = !showNameKeyboard }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = themeManager.pingDisplayName,
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showNameKeyboard,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            QwertyKeyboard(
                                searchQuery = themeManager.pingDisplayName,
                                showQueryText = false,
                                showFlipLayoutButton = false,
                                onQueryChange = { themeManager.updatePingDisplayName(it) },
                                keyboardFocusRequesters = keyboardFocusRequesters,
                                onFocusChanged = { focusedKeyIndex = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    PingAutoStartToggleItem()
                }

                if (receivedThemes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No themes received yet.\nBring a nearby device running this app within BLE range to receive themes.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        receivedThemes.forEach { (displayName, themes) ->
                            item {
                                Text(
                                    text = displayName,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp)
                                ) {
                                    items(themes) { theme ->
                                        ReceivedThemeCard(
                                            theme = theme,
                                            onClick = { themeManager.setTheme(theme) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedThemeCard(
    theme: ColorTheme,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val gradient = if (theme.isSolid) {
        Brush.linearGradient(listOf(theme.primaryColor, theme.primaryColor))
    } else {
        Brush.linearGradient(listOf(theme.primaryColor, theme.secondaryColor))
    }

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp)
            .scale(animatedFocusedScale(isFocused))
            .background(brush = gradient, shape = RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = theme.customName ?: "Unnamed",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
