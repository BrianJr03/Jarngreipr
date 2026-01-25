package jr.brian.home.util.shizuku

import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
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
        
        // For non-system keys (gamepad buttons), we need to dismiss the overlay first
        // so the button goes to the underlying app (e.g., a game)
        if (!isSystemKey && action == KeyEvent.ACTION_DOWN) {
            dismissOverlayFirst()
        }
        
        // Try InputManager first for all keys
        val success = injectViaInputManager(keyCode, action, isSystemKey)
        
        if (!success && action == KeyEvent.ACTION_DOWN) {
            // Only use shell fallback on DOWN since it sends both down+up
            Log.d(TAG, "InputManager failed, trying shell")
            injectViaShell(keyCode, isSystemKey)
        }
    }
    
    private fun dismissOverlayFirst() {
        try {
            Log.d(TAG, "Dismissing overlay with BACK key")
            
            // Create and inject a BACK key event to dismiss the control pad
            val im = inputManager
            val method = injectInputEventMethod
            
            if (im != null && method != null) {
                val now = SystemClock.uptimeMillis()
                
                // DOWN
                val downEvent = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK,
                    0, 0, 0, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD)
                method.invoke(im, downEvent, INJECT_INPUT_EVENT_MODE_ASYNC)
                
                Thread.sleep(50)
                
                // UP
                val upEvent = KeyEvent(now + 50, now + 50, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK,
                    0, 0, 0, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_KEYBOARD)
                method.invoke(im, upEvent, INJECT_INPUT_EVENT_MODE_ASYNC)
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent ${KeyEvent.KEYCODE_BACK}")).waitFor()
            }
            
            // Wait for focus to shift to the underlying app
            Thread.sleep(300)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss overlay", e)
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
}
