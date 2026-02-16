package jr.brian.home.esde.setup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import jr.brian.home.esde.scripts.ScriptManager
import java.io.File

object ESDESetupHelper {
    const val REQUEST_STORAGE_PERMISSION = 1001

    fun hasStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(activity: Activity) {
        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)

        activity.requestPermissions(permissions.toTypedArray(), REQUEST_STORAGE_PERMISSION)
    }

    fun initializeESDEIntegration(context: Context): SetupResult {
        return try {
            if (!hasStoragePermission(context)) {
                return SetupResult(
                    success = false,
                    message = "Storage permission required",
                    needsPermission = true
                )
            }

            val scriptsDir = File("/storage/emulated/0/ES-DE/scripts")
            if (!scriptsDir.exists()) {
                scriptsDir.mkdirs()
            }

            val scriptResult = ScriptManager.createAllScripts(scriptsDir)

            SetupResult(
                success = scriptResult.success,
                message = scriptResult.message,
                needsPermission = false
            )
        } catch (e: Exception) {
            SetupResult(
                success = false,
                message = "Failed to initialize ES-DE integration: ${e.message}",
                needsPermission = false
            )
        }
    }
}