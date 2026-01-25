package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import jr.brian.home.R
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.PhysicalButton
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalControlPadManager
import jr.brian.home.util.shizuku.ShizukuInputManager

@Composable
fun ControlPadScreen(
    onDismiss: () -> Unit = {}
) {
    val controlPadManager = LocalControlPadManager.current
    val controlPadItems by controlPadManager.controlPadItems.collectAsStateWithLifecycle()

    // Shizuku state
    val isShizukuAvailable by ShizukuInputManager.isShizukuAvailable.collectAsStateWithLifecycle()
    val isShizukuPermissionGranted by ShizukuInputManager.isShizukuPermissionGranted.collectAsStateWithLifecycle()
    val isServiceConnected by ShizukuInputManager.isServiceConnected.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedCardIndex by remember { mutableIntStateOf(-1) }
    var showButtonMappingDialog by remember { mutableStateOf(false) }

    // Initialize Shizuku when screen is shown
    DisposableEffect(Unit) {
        ShizukuInputManager.initialize()
        onDispose {
            // Don't cleanup here to keep service running
        }
    }

    BackHandler {
        if (isEditMode) {
            isEditMode = false
            selectedCardIndex = -1
        } else {
            onDismiss()
        }
    }

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..2) {
                        ControlPadCard(
                            item = controlPadItems[i],
                            isEditMode = isEditMode,
                            isSelected = selectedCardIndex == i,
                            onClick = {
                                if (isEditMode) {
                                    selectedCardIndex = i
                                    showButtonMappingDialog = true
                                } else {
                                    controlPadItems[i].mappedButton?.let { button ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            ShizukuInputManager.injectButtonPress(button)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 3..5) {
                        ControlPadCard(
                            item = controlPadItems[i],
                            isEditMode = isEditMode,
                            isSelected = selectedCardIndex == i,
                            onClick = {
                                if (isEditMode) {
                                    selectedCardIndex = i
                                    showButtonMappingDialog = true
                                } else {
                                    controlPadItems[i].mappedButton?.let { button ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            ShizukuInputManager.injectButtonPress(button)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Shizuku status indicator at top center
            ShizukuStatusIndicator(
                isShizukuAvailable = isShizukuAvailable,
                isPermissionGranted = isShizukuPermissionGranted,
                isServiceConnected = isServiceConnected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = isEditMode,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ControlPadActionButton(
                        text = stringResource(R.string.control_pad_reset),
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.control_pad_reset),
                        isHighlighted = false,
                        onClick = {
                            controlPadManager.resetAllMappings()
                        }
                    )
                }

                ControlPadActionButton(
                    text = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
                    icon = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
                    isHighlighted = isEditMode,
                    onClick = {
                        isEditMode = !isEditMode
                        if (!isEditMode) {
                            selectedCardIndex = -1
                        }
                    }
                )
            }
        }
    }

    if (showButtonMappingDialog && selectedCardIndex >= 0) {
        ButtonMappingDialog(
            currentMapping = controlPadItems[selectedCardIndex].mappedButton,
            onButtonSelected = { button ->
                controlPadManager.setControlPadItem(
                    index = selectedCardIndex,
                    item = ControlPadItem(
                        label = button.displayName,
                        mappedButton = button
                    )
                )
                showButtonMappingDialog = false
            },
            onDismiss = {
                showButtonMappingDialog = false
            }
        )
    }
}

@Composable
private fun ControlPadCard(
    item: ControlPadItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale - shrink slightly when pressed
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )

    // Animate vertical offset - move down when pressed
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 100),
        label = "offsetY"
    )

    val cardGradient = Brush.linearGradient(
        colors = if (isSelected || isPressed) {
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
        modifier = Modifier
            .size(100.dp)
            .offset(y = offsetY)
            .scale(pressScale)
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                brush = if (isSelected) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isEditMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.control_pad_tap_to_map),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ControlPadActionButton(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused || isHighlighted) {
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
        modifier = Modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused || isHighlighted) 3.dp else 2.dp,
                brush = if (isFocused || isHighlighted) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ButtonMappingDialog(
    currentMapping: PhysicalButton?,
    onButtonSelected: (PhysicalButton) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 16.dp)
                .background(
                    color = OledCardColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.control_pad_map_button_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.control_pad_close),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PhysicalButton.entries) { button ->
                        ButtonMappingOption(
                            button = button,
                            isSelected = currentMapping == button,
                            onClick = { onButtonSelected(button) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonMappingOption(
    button: PhysicalButton,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused || isSelected) {
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
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused || isSelected) 3.dp else 2.dp,
                brush = if (isFocused || isSelected) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.6f),
                            ThemeSecondaryColor.copy(alpha = 0.4f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .focusable()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.displayName,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ShizukuStatusIndicator(
    isShizukuAvailable: Boolean,
    isPermissionGranted: Boolean,
    isServiceConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        isServiceConnected -> Color(0xFF4CAF50) // Green
        isPermissionGranted -> Color(0xFFFFC107) // Yellow - connecting
        isShizukuAvailable -> Color(0xFFFF9800) // Orange - needs permission
        else -> Color(0xFFF44336) // Red - not available
    }

    val statusText = when {
        isServiceConnected -> stringResource(R.string.control_pad_shizuku_ready)
        isPermissionGranted -> stringResource(R.string.control_pad_shizuku_connecting)
        isShizukuAvailable -> stringResource(R.string.control_pad_shizuku_permission_required)
        else -> stringResource(R.string.control_pad_shizuku_not_running)
    }

    val statusIcon = when {
        isServiceConnected -> Icons.Default.Check
        isPermissionGranted -> Icons.Default.Refresh
        else -> Icons.Default.Lock
    }

    Row(
        modifier = modifier
            .background(
                color = OledCardColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = isShizukuAvailable && !isPermissionGranted) {
                ShizukuInputManager.requestPermission()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )

        Text(
            text = statusText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        if (isShizukuAvailable && !isPermissionGranted) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
