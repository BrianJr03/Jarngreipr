package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import jr.brian.home.data.JoystickMode
import jr.brian.home.model.ControlPadItem
import jr.brian.home.ui.components.controlpad.BottomActionButtons
import jr.brian.home.ui.components.controlpad.ButtonMappingDialog
import jr.brian.home.ui.components.controlpad.EditModeSettingsPanel
import jr.brian.home.ui.components.controlpad.GamePadButtonColumn
import jr.brian.home.ui.components.controlpad.GamePadHelpDialog
import jr.brian.home.ui.components.controlpad.ShizukuStatusIndicator
import jr.brian.home.ui.theme.OledBackgroundColor
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
                            val change = event.changes.firstOrNull() ?: continue
                            
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
                                                // Right stick - use ABSOLUTE offset from start
                                                touchStartPosition?.let { start ->
                                                    val offsetX = currentPos.x - start.x
                                                    val offsetY = currentPos.y - start.y
                                                    val x = (offsetX / joystickRadius).coerceIn(-1f, 1f)
                                                    val y = (offsetY / joystickRadius).coerceIn(-1f, 1f)
                                                    ShizukuInputManager.injectJoystick(x, y)
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
                                                    // Right stick (camera) - use ABSOLUTE offset from start
                                                    touchStartPosition?.let { start ->
                                                        val offsetX = currentPos.x - start.x
                                                        val offsetY = currentPos.y - start.y
                                                        val x = (offsetX / joystickRadius).coerceIn(-1f, 1f)
                                                        val y = (offsetY / joystickRadius).coerceIn(-1f, 1f)
                                                        ShizukuInputManager.injectJoystick(x, y)
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

private fun resetJoystickPositions() {
    ShizukuInputManager.injectLeftJoystick(0f, 0f)
    ShizukuInputManager.injectJoystick(0f, 0f)
}
