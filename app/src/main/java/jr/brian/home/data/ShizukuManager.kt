package jr.brian.home.data

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import jr.brian.home.BuildConfig
import jr.brian.home.IShellService
import jr.brian.home.service.ShellService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuManager @Inject constructor() {
    
    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()
    
    private val _isShizukuPermissionGranted = MutableStateFlow(false)
    val isShizukuPermissionGranted: StateFlow<Boolean> = _isShizukuPermissionGranted.asStateFlow()
    
    private var shellService: IShellService? = null
    
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
        bindShellService()
    }
    
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuAvailable.value = false
        _isShizukuPermissionGranted.value = false
        shellService = null
    }
    
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _isShizukuPermissionGranted.value = granted
        if (granted) {
            bindShellService()
        }
    }
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(1)
    
    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shellService = IShellService.Stub.asInterface(service)
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
        }
    }
    
    /**
     * Initialize Shizuku listeners. Call this when the app starts.
     */
    fun initialize() {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            
            // Check initial state
            if (Shizuku.pingBinder()) {
                _isShizukuAvailable.value = true
                checkShizukuPermission()
                if (_isShizukuPermissionGranted.value) {
                    bindShellService()
                }
            }
        } catch (e: Exception) {
            // Shizuku not installed
            _isShizukuAvailable.value = false
        }
    }
    
    /**
     * Clean up listeners and unbind service. Call this when the app is destroyed.
     */
    fun cleanup() {
        try {
            unbindShellService()
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Bind to the shell user service.
     */
    private fun bindShellService() {
        try {
            if (Shizuku.pingBinder() && _isShizukuPermissionGranted.value) {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (e: Exception) {
            // Ignore bind errors
        }
    }
    
    /**
     * Unbind from the shell user service.
     */
    private fun unbindShellService() {
        try {
            shellService?.destroy()
            Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
        } catch (e: Exception) {
            // Ignore unbind errors
        }
        shellService = null
    }
    
    /**
     * Check if Shizuku is installed on the device.
     */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check and update Shizuku permission state.
     */
    fun checkShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                _isShizukuAvailable.value = true
                _isShizukuPermissionGranted.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                _isShizukuAvailable.value = false
                _isShizukuPermissionGranted.value = false
            }
        } catch (e: Exception) {
            _isShizukuAvailable.value = false
            _isShizukuPermissionGranted.value = false
        }
    }
    
    /**
     * Request Shizuku permission.
     */
    fun requestPermission(requestCode: Int = SHIZUKU_PERMISSION_REQUEST_CODE) {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Force stop an app using Shizuku's UserService.
     * Returns true if successful, false otherwise.
     */
    fun forceStopApp(packageName: String): Boolean {
        if (!_isShizukuPermissionGranted.value) return false
        
        return try {
            val service = shellService
            if (service != null) {
                service.forceStop(packageName) == 0
            } else {
                // Try to bind and execute
                bindShellService()
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Force stop an app AND remove it from the system's recent apps screen.
     * Returns true if successful, false otherwise.
     */
    fun forceStopAndRemoveFromRecents(packageName: String): Boolean {
        if (!_isShizukuPermissionGranted.value) return false
        
        return try {
            val service = shellService
            if (service != null) {
                service.forceStopAndRemoveFromRecents(packageName) == 0
            } else {
                bindShellService()
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Fallback: Kill background processes (doesn't require Shizuku).
     * This is less effective than force-stop but works without special permissions.
     */
    fun killBackgroundProcesses(context: Context, packageName: String) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            // Ignore errors
        }
    }
    
    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
