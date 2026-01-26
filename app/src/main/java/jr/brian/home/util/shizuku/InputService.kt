package jr.brian.home.util.shizuku

import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import jr.brian.home.IInputService
import kotlin.system.exitProcess

class InputService : IInputService.Stub() {

    companion object {
        private const val TAG = "InputService"
        
        // Injection mode - ASYNC doesn't wait for result
        private const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        
        // Key codes that are system keys (work globally)
        private val SYSTEM_KEYS = setOf(
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_POWER
        )
    }
    
    private val inputManager: InputManager? by lazy {
        try {
            val getInstanceMethod = InputManager::class.java.getMethod("getInstance")
            getInstanceMethod.invoke(null) as? InputManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get InputManager instance", e)
            null
        }
    }
    
    private val injectInputEventMethod by lazy {
        try {
            InputManager::class.java.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get injectInputEvent method", e)
            null
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun injectKeyEvent(keyCode: Int, action: Int) {
        val isSystemKey = keyCode in SYSTEM_KEYS
        
        Log.d(TAG, "injectKeyEvent: keyCode=$keyCode, action=$action, isSystemKey=$isSystemKey")
        
        // Try InputManager first for all keys
        val success = injectViaInputManager(keyCode, action, isSystemKey)
        
        if (!success && action == KeyEvent.ACTION_DOWN) {
            // Only use shell fallback on DOWN since it sends both down+up
            Log.d(TAG, "InputManager failed, trying shell")
            injectViaShell(keyCode, isSystemKey)
        }
    }
    
    private fun injectViaInputManager(keyCode: Int, action: Int, isSystemKey: Boolean): Boolean {
        try {
            val im = inputManager
            val method = injectInputEventMethod
            
            if (im == null || method == null) {
                Log.e(TAG, "InputManager or method not available")
                return false
            }
            
            val now = SystemClock.uptimeMillis()
            
            // Use appropriate source based on key type
            val source = if (isSystemKey) {
                InputDevice.SOURCE_KEYBOARD
            } else {
                // For gamepad buttons, combine sources for better compatibility
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_DPAD
            }
            
            val event = KeyEvent(
                now,                        // downTime
                now,                        // eventTime  
                action,                     // action (DOWN or UP)
                keyCode,                    // keyCode
                0,                          // repeat
                0,                          // metaState
                0,                          // deviceId (0 = virtual)
                0,                          // scanCode
                KeyEvent.FLAG_FROM_SYSTEM,  // flags
                source                      // source
            )
            
            Log.d(TAG, "Injecting via InputManager: keyCode=$keyCode, action=$action, source=$source")
            val result = method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
            val success = result as? Boolean ?: true // Assume success if no boolean returned
            Log.d(TAG, "InputManager result: $success")
            
            return success
            
        } catch (e: Exception) {
            Log.e(TAG, "InputManager injection failed", e)
            return false
        }
    }
    
    private fun injectViaShell(keyCode: Int, isSystemKey: Boolean) {
        try {
            val command = "input keyevent $keyCode"
            Log.d(TAG, "Executing shell: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            Log.d(TAG, "Shell exit code: $exitCode")
        } catch (e: Exception) {
            Log.e(TAG, "Shell injection failed", e)
        }
    }
    
    override fun injectTrigger(axis: Int, value: Float) {
        Log.d(TAG, "injectTrigger: axis=$axis, value=$value")
        
        try {
            val im = inputManager
            val method = injectInputEventMethod
            
            if (im == null || method == null) {
                Log.e(TAG, "InputManager not available for trigger injection")
                return
            }
            
            val now = SystemClock.uptimeMillis()
            
            // Create pointer properties
            val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_UNKNOWN
            })
            
            // Create pointer coords with the trigger axis value
            val pointerCoords = arrayOf(MotionEvent.PointerCoords().apply {
                x = 0f
                y = 0f
                pressure = 0f
                size = 0f
                setAxisValue(axis, value)
            })
            
            // Use SOURCE_JOYSTICK - this is what real gamepads report for analog triggers
            val source = InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_GAMEPAD
            
            val event = MotionEvent.obtain(
                now,                                    // downTime
                now,                                    // eventTime
                MotionEvent.ACTION_MOVE,                // action
                1,                                      // pointerCount
                pointerProperties,                      // pointer properties
                pointerCoords,                          // pointer coords
                0,                                      // metaState
                0,                                      // buttonState
                1f,                                     // xPrecision
                1f,                                     // yPrecision
                0,                                      // deviceId (0 = virtual)
                0,                                      // edgeFlags
                source,                                 // source
                0                                       // flags
            )
            
            Log.d(TAG, "Injecting trigger MotionEvent: axis=$axis, value=$value, source=$source")
            val result = method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
            Log.d(TAG, "Trigger injection result: $result")
            
            event.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Trigger injection failed", e)
        }
    }
    
    override fun injectJoystick(rightX: Float, rightY: Float) {
        Log.d(TAG, "injectJoystick: rightX=$rightX, rightY=$rightY")
        
        try {
            val im = inputManager
            val method = injectInputEventMethod
            
            if (im == null || method == null) {
                Log.e(TAG, "InputManager not available for joystick injection")
                return
            }
            
            val now = SystemClock.uptimeMillis()
            
            // Create pointer properties
            val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_UNKNOWN
            })
            
            // Create pointer coords with right stick axis values
            // AXIS_Z = right stick X, AXIS_RZ = right stick Y (most common mapping)
            val pointerCoords = arrayOf(MotionEvent.PointerCoords().apply {
                x = 0f
                y = 0f
                pressure = 0f
                size = 0f
                setAxisValue(MotionEvent.AXIS_Z, rightX)
                setAxisValue(MotionEvent.AXIS_RZ, rightY)
                // Also set RX/RY as some games use these instead
                setAxisValue(MotionEvent.AXIS_RX, rightX)
                setAxisValue(MotionEvent.AXIS_RY, rightY)
            })
            
            val source = InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_GAMEPAD
            
            val event = MotionEvent.obtain(
                now,                                    // downTime
                now,                                    // eventTime
                MotionEvent.ACTION_MOVE,                // action
                1,                                      // pointerCount
                pointerProperties,                      // pointer properties
                pointerCoords,                          // pointer coords
                0,                                      // metaState
                0,                                      // buttonState
                1f,                                     // xPrecision
                1f,                                     // yPrecision
                0,                                      // deviceId (0 = virtual)
                0,                                      // edgeFlags
                source,                                 // source
                0                                       // flags
            )
            
            Log.d(TAG, "Injecting joystick MotionEvent: rightX=$rightX, rightY=$rightY")
            val result = method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
            Log.d(TAG, "Joystick injection result: $result")
            
            event.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Joystick injection failed", e)
        }
    }
    
    override fun injectLeftJoystick(leftX: Float, leftY: Float) {
        Log.d(TAG, "injectLeftJoystick: leftX=$leftX, leftY=$leftY")
        
        try {
            val im = inputManager
            val method = injectInputEventMethod
            
            if (im == null || method == null) {
                Log.e(TAG, "InputManager not available for left joystick injection")
                return
            }
            
            val now = SystemClock.uptimeMillis()
            
            val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_UNKNOWN
            })
            
            // Left stick uses AXIS_X and AXIS_Y
            val pointerCoords = arrayOf(MotionEvent.PointerCoords().apply {
                x = 0f
                y = 0f
                pressure = 0f
                size = 0f
                setAxisValue(MotionEvent.AXIS_X, leftX)
                setAxisValue(MotionEvent.AXIS_Y, leftY)
            })
            
            val source = InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_GAMEPAD
            
            val event = MotionEvent.obtain(
                now,
                now,
                MotionEvent.ACTION_MOVE,
                1,
                pointerProperties,
                pointerCoords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                source,
                0
            )
            
            Log.d(TAG, "Injecting left joystick MotionEvent: leftX=$leftX, leftY=$leftY")
            val result = method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
            Log.d(TAG, "Left joystick injection result: $result")
            
            event.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Left joystick injection failed", e)
        }
    }
}
