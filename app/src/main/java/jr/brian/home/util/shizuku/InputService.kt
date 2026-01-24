package jr.brian.home.util.shizuku

import android.util.Log
import android.view.KeyEvent
import jr.brian.home.IInputService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class InputService : IInputService.Stub() {

    companion object {
        private const val TAG = "InputService"
        
        // Key codes that should use keyboard source instead of gamepad
        private val KEYBOARD_KEYS = setOf(
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_POWER
        )
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun injectKeyEvent(keyCode: Int, action: Int) {
        // Only inject on ACTION_DOWN to avoid double-triggering
        // The shell command handles both down and up
        if (action != KeyEvent.ACTION_DOWN) {
            return
        }
        
        try {
            val isSystemKey = keyCode in KEYBOARD_KEYS
            
            // Use shell command for input injection - works better with emulators
            // For gamepad buttons, use the gamepad source flag
            val command = if (isSystemKey) {
                // System keys use regular input keyevent
                "input keyevent $keyCode"
            } else {
                // Gamepad buttons - use input with source flag
                // source 0x401 = gamepad
                "input keyevent --source 0x401 $keyCode"
            }
            
            Log.d(TAG, "Executing: $command")
            
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val error = errorReader.readText()
                Log.e(TAG, "Command failed with exit code $exitCode: $error")
            } else {
                Log.d(TAG, "Command succeeded for keyCode=$keyCode")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject key event: keyCode=$keyCode, action=$action", e)
        }
    }
}
