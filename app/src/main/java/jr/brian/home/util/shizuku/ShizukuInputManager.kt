package jr.brian.home.util.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import jr.brian.home.BuildConfig
import jr.brian.home.IInputService
import jr.brian.home.model.PhysicalButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuInputManager {

    private const val TAG = "ShizukuInputManager"

    private var inputService: IInputService? = null
    private var isBound = false

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _isShizukuPermissionGranted = MutableStateFlow(false)
    val isShizukuPermissionGranted: StateFlow<Boolean> = _isShizukuPermissionGranted.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, InputService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("input_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected: $name, binder: $service")
            inputService = IInputService.Stub.asInterface(service)
            isBound = true
            _isServiceConnected.value = true
            Log.d(TAG, "inputService initialized: ${inputService != null}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected: $name")
            inputService = null
            isBound = false
            _isServiceConnected.value = false
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isShizukuAvailable.value = true
        checkPermission()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuAvailable.value = false
        _isShizukuPermissionGranted.value = false
        _isServiceConnected.value = false
        inputService = null
        isBound = false
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _isShizukuPermissionGranted.value = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (_isShizukuPermissionGranted.value) {
            bindService()
        }
    }

    fun initialize() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun cleanup() {
        unbindService()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    fun checkPermission() {
        if (!Shizuku.pingBinder()) {
            _isShizukuAvailable.value = false
            _isShizukuPermissionGranted.value = false
            return
        }

        _isShizukuAvailable.value = true

        try {
            if (Shizuku.isPreV11()) {
                _isShizukuPermissionGranted.value = false
            } else if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                _isShizukuPermissionGranted.value = true
                if (!isBound) {
                    bindService()
                }
            } else {
                _isShizukuPermissionGranted.value = false
            }
        } catch (e: Exception) {
            _isShizukuPermissionGranted.value = false
        }
    }

    fun requestPermission(requestCode: Int = SHIZUKU_PERMISSION_REQUEST_CODE) {
        if (!Shizuku.pingBinder()) return

        try {
            if (!Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bindService() {
        Log.d(TAG, "bindService called")
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            Log.d(TAG, "bindUserService called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "bindService failed", e)
        }
    }

    private fun unbindService() {
        if (isBound) {
            try {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isBound = false
            inputService = null
            _isServiceConnected.value = false
        }
    }

    fun injectButtonPress(button: PhysicalButton): Boolean {
        Log.d(TAG, "injectButtonPress called: button=${button.displayName}, keyCode=${button.keyCode}")
        Log.d(TAG, "inputService=$inputService, isBound=$isBound, isServiceConnected=${_isServiceConnected.value}")
        
        return try {
            val service = inputService
            if (service == null) {
                Log.e(TAG, "inputService is NULL - cannot inject")
                return false
            }
            
            Log.d(TAG, "Calling injectKeyEvent DOWN for ${button.displayName}")
            service.injectKeyEvent(button.keyCode, KeyEvent.ACTION_DOWN)
            
            Log.d(TAG, "Calling injectKeyEvent UP for ${button.displayName}")
            service.injectKeyEvent(button.keyCode, KeyEvent.ACTION_UP)
            
            Log.d(TAG, "injectButtonPress completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "injectButtonPress failed", e)
            false
        }
    }

    fun injectKeyDown(button: PhysicalButton): Boolean {
        return try {
            inputService?.injectKeyEvent(button.keyCode, KeyEvent.ACTION_DOWN)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun injectKeyUp(button: PhysicalButton): Boolean {
        return try {
            inputService?.injectKeyEvent(button.keyCode, KeyEvent.ACTION_UP)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun injectTriggerPress(button: PhysicalButton): Boolean {
        val axis = when (button) {
            PhysicalButton.L2 -> MotionEvent.AXIS_LTRIGGER
            PhysicalButton.R2 -> MotionEvent.AXIS_RTRIGGER
            else -> return false
        }
        
        return try {
            Log.d(TAG, "Injecting trigger press: ${button.displayName}, axis=$axis")
            // Send both KeyEvent AND MotionEvent for maximum compatibility
            // Some games expect digital button, others expect analog axis
            inputService?.injectKeyEvent(button.keyCode, KeyEvent.ACTION_DOWN)
            inputService?.injectTrigger(axis, 1.0f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Trigger press failed", e)
            false
        }
    }
    
    fun injectTriggerRelease(button: PhysicalButton): Boolean {
        val axis = when (button) {
            PhysicalButton.L2 -> MotionEvent.AXIS_LTRIGGER
            PhysicalButton.R2 -> MotionEvent.AXIS_RTRIGGER
            else -> return false
        }
        
        return try {
            Log.d(TAG, "Injecting trigger release: ${button.displayName}, axis=$axis")
            // Send both KeyEvent AND MotionEvent for maximum compatibility
            inputService?.injectKeyEvent(button.keyCode, KeyEvent.ACTION_UP)
            inputService?.injectTrigger(axis, 0.0f)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Trigger release failed", e)
            false
        }
    }
    
    fun isTriggerButton(button: PhysicalButton): Boolean {
        return button == PhysicalButton.L2 || button == PhysicalButton.R2
    }

    const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
}
