package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import jr.brian.home.data.ControlPadManager
import jr.brian.home.data.JoystickMode
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.PhysicalButton
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.StatusGreen
import jr.brian.home.ui.theme.StatusOrange
import jr.brian.home.ui.theme.StatusRed
import jr.brian.home.ui.theme.StatusYellow
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalControlPadManager
import jr.brian.home.util.shizuku.ShizukuInputManager

@Composable
fun GamePadScreen(
    onDismiss: () -> Unit = {}
) {
    val gamePadManager = LocalControlPadManager.current
    val gamePadItems by gamePadManager.controlPadItems.collectAsStateWithLifecycle()
    val cameraSensitivity by gamePadManager.cameraSensitivity.collectAsStateWithLifecycle()
    val joystickMode by gamePadManager.joystickMode.collectAsStateWithLifecycle()

    val isShizukuAvailable by ShizukuInputManager.isShizukuAvailable.collectAsStateWithLifecycle()
    val isShizukuPermissionGranted by ShizukuInputManager.isShizukuPermissionGranted.collectAsStateWithLifecycle()
    val isServiceConnected by ShizukuInputManager.isServiceConnected.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedCardIndex by remember { mutableIntStateOf(-1) }
    var showButtonMappingDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        ShizukuInputManager.initialize()
        onDispose {
            resetJoystickPositions()
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

    // For camera (right stick) - track last position for delta calculation
    var lastTouchPosition by remember { mutableStateOf<Offset?>(null) }
    // For movement (left stick) - track where touch started for absolute offset
    var touchStartPosition by remember { mutableStateOf<Offset?>(null) }
    var touchStartedOnLeft by remember { mutableStateOf(false) }
    
    // Virtual joystick radius - how far finger can move from start point
    val joystickRadius = 100f
    
    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .padding(vertical = 8.dp)
                .pointerInput(joystickMode, cameraSensitivity) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            
                            if (change != null && !change.isConsumed) {
                                val screenWidth = size.width
                                
                                when {
                                    change.pressed && touchStartPosition == null -> {
                                        // Started touching - determine which side and store start position
                                        touchStartPosition = change.position
                                        lastTouchPosition = change.position
                                        touchStartedOnLeft = change.position.x < screenWidth / 2
                                    }
                                    change.pressed && touchStartPosition != null -> {
                                        val currentPos = change.position
                                        
                                        coroutineScope.launch(Dispatchers.IO) {
                                            when (joystickMode) {
                                                JoystickMode.LEFT_ONLY -> {
                                                    // Left stick only - use ABSOLUTE offset from start
                                                    touchStartPosition?.let { start ->
                                                        val offsetX = currentPos.x - start.x
                                                        val offsetY = currentPos.y - start.y
                                                        val x = (offsetX / joystickRadius).coerceIn(-1f, 1f)
                                                        val y = (offsetY / joystickRadius).coerceIn(-1f, 1f)
                                                        ShizukuInputManager.injectLeftJoystick(x, y)
                                                    }
                                                }
                                                JoystickMode.RIGHT_ONLY -> {
                                                    // Camera uses delta (trackpad-like)
                                                    lastTouchPosition?.let { last ->
                                                        val deltaX = currentPos.x - last.x
                                                        val deltaY = currentPos.y - last.y
                                                        if (deltaX != 0f || deltaY != 0f) {
                                                            val x = (deltaX * cameraSensitivity).coerceIn(-1f, 1f)
                                                            val y = (deltaY * cameraSensitivity).coerceIn(-1f, 1f)
                                                            ShizukuInputManager.injectJoystick(x, y)
                                                        }
                                                    }
                                                }
                                                JoystickMode.LEFT_RIGHT -> {
                                                    if (touchStartedOnLeft) {
                                                        // Left stick (movement) - use ABSOLUTE offset from start
                                                        touchStartPosition?.let { start ->
                                                            val offsetX = currentPos.x - start.x
                                                            val offsetY = currentPos.y - start.y
                                                            val x = (offsetX / joystickRadius).coerceIn(-1f, 1f)
                                                            val y = (offsetY / joystickRadius).coerceIn(-1f, 1f)
                                                            ShizukuInputManager.injectLeftJoystick(x, y)
                                                        }
                                                    } else {
                                                        // Right stick (camera) - use delta (trackpad-like)
                                                        lastTouchPosition?.let { last ->
                                                            val deltaX = currentPos.x - last.x
                                                            val deltaY = currentPos.y - last.y
                                                            if (deltaX != 0f || deltaY != 0f) {
                                                                val x = (deltaX * cameraSensitivity).coerceIn(-1f, 1f)
                                                                val y = (deltaY * cameraSensitivity).coerceIn(-1f, 1f)
                                                                ShizukuInputManager.injectJoystick(x, y)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        lastTouchPosition = currentPos
                                    }
                                    !change.pressed && touchStartPosition != null -> {
                                        touchStartPosition = null
                                        lastTouchPosition = null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            when (joystickMode) {
                                                JoystickMode.LEFT_ONLY -> {
                                                    ShizukuInputManager.injectLeftJoystick(0f, 0f)
                                                }
                                                JoystickMode.RIGHT_ONLY -> {
                                                    ShizukuInputManager.injectJoystick(0f, 0f)
                                                }
                                                JoystickMode.LEFT_RIGHT -> {
                                                    if (touchStartedOnLeft) {
                                                        ShizukuInputManager.injectLeftJoystick(0f, 0f)
                                                    } else {
                                                        ShizukuInputManager.injectJoystick(0f, 0f)
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
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GamePadButtonColumn(
                    gamePadItems = gamePadItems,
                    indices = 0..2,
                    isEditMode = isEditMode,
                    selectedCardIndex = selectedCardIndex,
                    onCardPress = { index ->
                        if (isEditMode) {
                            selectedCardIndex = index
                            showButtonMappingDialog = true
                        } else {
                            gamePadItems[index].mappedButton?.let { button ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (ShizukuInputManager.isTriggerButton(button)) {
                                        ShizukuInputManager.injectTriggerPress(button)
                                    } else {
                                        ShizukuInputManager.injectKeyDown(button)
                                    }
                                }
                            }
                        }
                    },
                    onCardRelease = { index ->
                        if (!isEditMode) {
                            gamePadItems[index].mappedButton?.let { button ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (ShizukuInputManager.isTriggerButton(button)) {
                                        ShizukuInputManager.injectTriggerRelease(button)
                                    } else {
                                        ShizukuInputManager.injectKeyUp(button)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )

                GamePadButtonColumn(
                    gamePadItems = gamePadItems,
                    indices = 3..5,
                    isEditMode = isEditMode,
                    selectedCardIndex = selectedCardIndex,
                    onCardPress = { index ->
                        if (isEditMode) {
                            selectedCardIndex = index
                            showButtonMappingDialog = true
                        } else {
                            gamePadItems[index].mappedButton?.let { button ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (ShizukuInputManager.isTriggerButton(button)) {
                                        ShizukuInputManager.injectTriggerPress(button)
                                    } else {
                                        ShizukuInputManager.injectKeyDown(button)
                                    }
                                }
                            }
                        }
                    },
                    onCardRelease = { index ->
                        if (!isEditMode) {
                            gamePadItems[index].mappedButton?.let { button ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (ShizukuInputManager.isTriggerButton(button)) {
                                        ShizukuInputManager.injectTriggerRelease(button)
                                    } else {
                                        ShizukuInputManager.injectKeyUp(button)
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            ShizukuStatusIndicator(
                isShizukuAvailable = isShizukuAvailable,
                isPermissionGranted = isShizukuPermissionGranted,
                isServiceConnected = isServiceConnected,
                onHelpClick = { showHelpDialog = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
            
            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
            ) {
                EditModeSettingsPanel(
                    joystickMode = joystickMode,
                    cameraSensitivity = cameraSensitivity,
                    onJoystickModeSelected = { gamePadManager.setJoystickMode(it) },
                    onSensitivityChange = { gamePadManager.setCameraSensitivity(it) }
                )
            }
            
            BottomActionButtons(
                isEditMode = isEditMode,
                onResetClick = { gamePadManager.resetAllMappings() },
                onEditToggle = {
                    isEditMode = !isEditMode
                    if (!isEditMode) {
                        selectedCardIndex = -1
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    if (showButtonMappingDialog && selectedCardIndex >= 0) {
        ButtonMappingDialog(
            currentMapping = gamePadItems[selectedCardIndex].mappedButton,
            onButtonSelected = { button ->
                gamePadManager.setControlPadItem(
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

    if (showHelpDialog) {
        GamePadHelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
private fun GamePadCard(
    item: ControlPadItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed)

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
                width = if (isSelected || isPressed) 3.dp else 2.dp,
                brush = if (isSelected || isPressed) {
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
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when {
                            event.changes.any { it.pressed } && !isPressed -> {
                                isPressed = true
                                onPress()
                            }
                            event.changes.none { it.pressed } && isPressed -> {
                                isPressed = false
                                onRelease()
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
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
private fun GamePadActionButton(
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
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        isServiceConnected -> StatusGreen
        isPermissionGranted -> StatusYellow
        isShizukuAvailable -> StatusOrange
        else -> StatusRed
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
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
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

        Box(
            modifier = Modifier
                .background(
                    color = OledCardColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable { onHelpClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.control_pad_help),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun JoystickModeOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) ThemePrimaryColor.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(3.dp)
                .background(
                    if (isSelected) ThemePrimaryColor else Color.Transparent,
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun JoystickModeSelector(
    joystickMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.control_pad_joystick),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_l),
                isSelected = joystickMode == JoystickMode.LEFT_ONLY,
                onClick = { onModeSelected(JoystickMode.LEFT_ONLY) }
            )
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_lr),
                isSelected = joystickMode == JoystickMode.LEFT_RIGHT,
                onClick = { onModeSelected(JoystickMode.LEFT_RIGHT) }
            )
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_r),
                isSelected = joystickMode == JoystickMode.RIGHT_ONLY,
                onClick = { onModeSelected(JoystickMode.RIGHT_ONLY) }
            )
        }
    }
}

@Composable
private fun SensitivitySlider(
    cameraSensitivity: Float,
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.control_pad_camera_sensitivity),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = cameraSensitivity,
            onValueChange = onSensitivityChange,
            valueRange = ControlPadManager.MIN_CAMERA_SENSITIVITY..ControlPadManager.MAX_CAMERA_SENSITIVITY,
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        )
        Text(
            text = "%.3f".format(cameraSensitivity),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EditModeSettingsPanel(
    joystickMode: JoystickMode,
    cameraSensitivity: Float,
    onJoystickModeSelected: (JoystickMode) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.55f)
            .background(
                color = OledCardColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.6f),
                        ThemeSecondaryColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        JoystickModeSelector(
            joystickMode = joystickMode,
            onModeSelected = onJoystickModeSelected
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        SensitivitySlider(
            cameraSensitivity = cameraSensitivity,
            onSensitivityChange = onSensitivityChange
        )
    }
}

@Composable
private fun BottomActionButtons(
    isEditMode: Boolean,
    onResetClick: () -> Unit,
    onEditToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            GamePadActionButton(
                text = stringResource(R.string.control_pad_reset),
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.control_pad_reset),
                isHighlighted = false,
                onClick = onResetClick
            )
        }

        GamePadActionButton(
            text = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
            icon = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
            contentDescription = if (isEditMode) stringResource(R.string.control_pad_done) else stringResource(R.string.control_pad_edit),
            isHighlighted = isEditMode,
            onClick = onEditToggle
        )
    }
}

@Composable
private fun GamePadButtonColumn(
    gamePadItems: List<ControlPadItem>,
    indices: IntRange,
    isEditMode: Boolean,
    selectedCardIndex: Int,
    onCardPress: (Int) -> Unit,
    onCardRelease: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in indices) {
            GamePadCard(
                item = gamePadItems[i],
                isEditMode = isEditMode,
                isSelected = selectedCardIndex == i,
                onPress = { onCardPress(i) },
                onRelease = { onCardRelease(i) }
            )
        }
    }
}

@Composable
private fun GamePadHelpDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
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
                modifier = Modifier.fillMaxSize(),
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
                        text = stringResource(R.string.control_pad_help_title),
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

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Shizuku Section
                    item {
                        HelpSectionHeader(text = stringResource(R.string.control_pad_help_section_shizuku))
                    }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_step_1)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_step_2)) }

                    // Termux Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HelpSectionHeader(text = stringResource(R.string.control_pad_help_section_termux))
                    }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_1)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_2)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_3)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_4)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_5)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_6)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_termux_7)) }

                    // Finish Setup Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HelpSectionHeader(text = stringResource(R.string.control_pad_help_section_finish))
                    }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_finish_1)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_finish_2)) }
                    item { HelpStepItem(text = stringResource(R.string.control_pad_help_finish_3)) }

                    // Note Section
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = StatusOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = StatusOrange.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.control_pad_help_note_title),
                                color = StatusOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.control_pad_help_note),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Emulator Compatibility Note
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = StatusYellow.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = StatusYellow.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.control_pad_help_emulator_title),
                                color = StatusYellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.control_pad_help_emulator_note),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSectionHeader(text: String) {
    Text(
        text = text,
        color = ThemePrimaryColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun HelpStepItem(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun resetJoystickPositions() {
    ShizukuInputManager.injectLeftJoystick(0f, 0f)
    ShizukuInputManager.injectJoystick(0f, 0f)
}
