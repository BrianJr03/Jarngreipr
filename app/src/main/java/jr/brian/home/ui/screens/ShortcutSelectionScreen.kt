package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.Shortcut
import jr.brian.home.model.ShortcutOption
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.components.dialog.AppSelectionDialog
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.util.rememberDialogState

/**
 * Base composable for shortcut selection screens.
 * Can be reused for back button shortcuts, marquee press shortcuts, etc.
 */
@Composable
fun ShortcutSelectionScreen(
    title: String,
    description: String,
    currentShortcut: Shortcut,
    currentAppPackage: String?,
    allApps: List<AppInfo>,
    shortcuts: List<ShortcutOption>,
    onShortcutSelected: (Shortcut) -> Unit,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val appSelectionDialogState = rememberDialogState<Unit>()

    BackHandler(onBack = onDismiss)

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
                ScreenHeader(onBackClick = onDismiss)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        color = ThemeSecondaryColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(shortcuts) { option ->
                        ShortcutOptionItem(
                            label = option.label,
                            isSelected = currentShortcut == option.shortcut,
                            onClick = {
                                onShortcutSelected(option.shortcut)
                                if (option.shortcut == Shortcut.APP) {
                                    appSelectionDialogState.show()
                                } else {
                                    onDismiss()
                                }
                            }
                        )
                    }

                    if (currentShortcut == Shortcut.APP) {
                        item {
                            val selectedApp = allApps.find { it.packageName == currentAppPackage }
                            val displayText = if (selectedApp != null) {
                                stringResource(
                                    R.string.shortcut_selected_app,
                                    selectedApp.label
                                )
                            } else {
                                stringResource(R.string.shortcut_no_app)
                            }

                            ShortcutAppSelectionItem(
                                text = displayText,
                                onClick = {
                                    appSelectionDialogState.show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (appSelectionDialogState.isVisible) {
        AppSelectionDialog(
            apps = allApps,
            onAppSelected = { app ->
                onAppSelected(app.packageName)
                appSelectionDialogState.dismiss()
            },
            onDismiss = appSelectionDialogState::dismiss
        )
    }
}

@Composable
internal fun ShortcutOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isSelected -> Color.White
        isFocused -> Color.LightGray.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    val borderWidth = if (isSelected || isFocused) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = subtleCardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
internal fun ShortcutAppSelectionItem(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = subtleCardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.LightGray.copy(alpha = 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ThemeSecondaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
