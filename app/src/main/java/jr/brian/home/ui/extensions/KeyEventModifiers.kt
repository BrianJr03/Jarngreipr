package jr.brian.home.ui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Handles select button (ButtonSelect or ButtonStart) press events
 * @param onSelectPress Callback when select button is pressed
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleSelectButton(onSelectPress: () -> Unit): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonSelect || event.key == Key.ButtonStart) -> {
                onSelectPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles directional navigation and select button press events
 * @param onNavigateUp Callback when up direction is pressed (optional)
 * @param onNavigateDown Callback when down direction is pressed (optional)
 * @param onSelectPress Callback when select button is pressed (optional)
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleNavigationAndSelect(
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onSelectPress: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp &&
                    onNavigateUp != null -> {
                onNavigateUp()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    onNavigateDown != null -> {
                onNavigateDown()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonSelect || event.key == Key.ButtonStart) &&
                    onSelectPress != null -> {
                onSelectPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles D-pad navigation with enter key support
 * @param onNavigateUp Callback when up direction is pressed (optional)
 * @param onNavigateDown Callback when down direction is pressed (optional)
 * @param onEnterPress Callback when enter key is pressed (optional)
 * @return Modifier with key event handling
 */
fun Modifier.handleDPadNavigation(
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onEnterPress: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp &&
                    onNavigateUp != null -> {
                onNavigateUp()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    onNavigateDown != null -> {
                onNavigateDown()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.Enter &&
                    onEnterPress != null -> {
                onEnterPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles full directional navigation with enter and menu key support
 * @param onNavigateUp Callback when up direction is pressed (optional)
 * @param onNavigateDown Callback when down direction is pressed (optional)
 * @param onNavigateLeft Callback when left direction is pressed (optional)
 * @param onNavigateRight Callback when right direction is pressed (optional)
 * @param onEnterPress Callback when enter key is pressed (optional)
 * @param onMenuPress Callback when menu key is pressed (optional)
 * @return Modifier with key event handling
 */
fun Modifier.handleFullNavigation(
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onEnterPress: (() -> Unit)? = null,
    onMenuPress: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp &&
                    onNavigateUp != null -> {
                onNavigateUp()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    onNavigateDown != null -> {
                onNavigateDown()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    onNavigateLeft != null -> {
                onNavigateLeft()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight &&
                    onNavigateRight != null -> {
                onNavigateRight()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.Enter &&
                    onEnterPress != null -> {
                onEnterPress()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.Menu &&
                    onMenuPress != null -> {
                onMenuPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles complex navigation with enter on KeyDown and menu on KeyUp
 * @param onNavigateUp Callback when up direction is pressed (optional)
 * @param onNavigateDown Callback when down direction is pressed (optional)
 * @param onNavigateLeft Callback when left direction is pressed (optional)
 * @param onNavigateRight Callback when right direction is pressed (optional)
 * @param onEnterPress Callback when enter key is pressed on KeyDown (optional)
 * @param onMenuPress Callback when menu key is released on KeyUp (optional)
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleComplexNavigation(
    onNavigateUp: (() -> Unit)? = null,
    onNavigateDown: (() -> Unit)? = null,
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onEnterPress: (() -> Unit)? = null,
    onMenuPress: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionUp &&
                    onNavigateUp != null -> {
                onNavigateUp()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    onNavigateDown != null -> {
                onNavigateDown()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    onNavigateLeft != null -> {
                onNavigateLeft()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight &&
                    onNavigateRight != null -> {
                onNavigateRight()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.Enter &&
                    onEnterPress != null -> {
                onEnterPress()
                true
            }

            event.type == KeyEventType.KeyUp &&
                    event.key == Key.Menu &&
                    onMenuPress != null -> {
                onMenuPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles only enter key press
 * @param onEnterPress Callback when enter key is pressed
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleEnterKey(onEnterPress: () -> Unit): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown && event.key == Key.Enter -> {
                onEnterPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles shoulder button navigation (L1/L2/R1/R2)
 * @param onLeftShoulder Callback when left shoulder buttons (L1/L2) are pressed (optional)
 * @param onRightShoulder Callback when right shoulder buttons (R1/R2) are pressed (optional)
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleShoulderButtons(
    onLeftShoulder: (() -> Unit)? = null,
    onRightShoulder: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonL1 || event.key == Key.ButtonL2) &&
                    onLeftShoulder != null -> {
                onLeftShoulder()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonR1 || event.key == Key.ButtonR2) &&
                    onRightShoulder != null -> {
                onRightShoulder()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles horizontal navigation (left/right) with enter key
 * @param onNavigateLeft Callback when left direction is pressed (optional)
 * @param onNavigateRight Callback when right direction is pressed (optional)
 * @param onEnterPress Callback when enter key is pressed (optional)
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleHorizontalNavigationWithEnter(
    onNavigateLeft: (() -> Unit)? = null,
    onNavigateRight: (() -> Unit)? = null,
    onEnterPress: (() -> Unit)? = null,
): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    onNavigateLeft != null -> {
                onNavigateLeft()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionRight &&
                    onNavigateRight != null -> {
                onNavigateRight()
                true
            }

            event.type == KeyEventType.KeyDown &&
                    event.key == Key.Enter &&
                    onEnterPress != null -> {
                onEnterPress()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles right directional navigation only
 * @param onNavigateRight Callback when right direction is pressed
 * @return Modifier with key event handling
 */
fun Modifier.handleRightNavigation(onNavigateRight: () -> Unit): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight -> {
                onNavigateRight()
                true
            }

            else -> false
        }
    }
}

/**
 * Handles left directional navigation only
 * @param onNavigateLeft Callback when left direction is pressed
 * @return Modifier with key event handling
 */
@Suppress("unused")
fun Modifier.handleLeftNavigation(onNavigateLeft: () -> Unit): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft -> {
                onNavigateLeft()
                true
            }

            else -> false
        }
    }
}

/**
 * Blocks horizontal (left/right) directional navigation
 * @return Modifier that consumes left/right directional key events
 */
fun Modifier.blockHorizontalNavigation(): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) -> {
                true
            }
            else -> false
        }
    }
}

/**
 * Blocks vertical (up/down) directional navigation
 * @return Modifier that consumes up/down directional key events
 */
@Suppress("unused")
fun Modifier.blockVerticalNavigation(): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionUp || event.key == Key.DirectionDown) -> {
                true
            }

            else -> false
        }
    }
}

/**
 * Blocks all directional navigation (up/down/left/right)
 * @return Modifier that consumes all directional key events
 */
fun Modifier.blockAllNavigation(): Modifier {
    return this.onKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionUp ||
                            event.key == Key.DirectionDown ||
                            event.key == Key.DirectionLeft ||
                            event.key == Key.DirectionRight) -> {
                true
            }
            else -> false
        }
    }
}