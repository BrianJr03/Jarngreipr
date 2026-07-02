package jr.brian.home.esde.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File
import jr.brian.home.esde.model.SetupStep

class SetupWizardManager(
    private val context: Context,
    private val preferences: SetupPreferences,
    private val onStepChanged: (SetupStep) -> Unit,
    private val onWizardComplete: () -> Unit
) {
    private var currentStep = 0
    var isInSetupWizard = false
        private set

    fun cancelWizard() {
        isInSetupWizard = false
        currentStep = 0
    }

    fun continueWizard() {
        currentStep++

        when (currentStep) {
            1 -> {
                if (hasStoragePermission()) {
                    onStepChanged(SetupStep.PermissionsGranted)
                } else {
                    onStepChanged(SetupStep.RequestPermissions)
                }
            }
            2 -> onStepChanged(SetupStep.SelectScriptsFolder)
            3 -> onStepChanged(SetupStep.CreateScripts)
            4 -> onStepChanged(SetupStep.SelectMediaFolder)
            5 -> onStepChanged(SetupStep.EnableScriptsInESDE)
            6 -> {
                isInSetupWizard = false
                preferences.setupCompleted = true
                onWizardComplete()
            }
        }
    }

    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }
    }

    fun isValidScriptsPath(path: String): Boolean {
        return path.contains("ES-DE", ignoreCase = true) &&
                path.contains("scripts", ignoreCase = true)
    }

    fun isValidMediaPath(path: String): Boolean {
        return path.contains("downloaded_media", ignoreCase = true)
    }

    fun areScriptsInstalled(scriptsPath: String): Boolean {
        val scriptsDir = File(scriptsPath)
        val scriptFiles = listOf(
            File(scriptsDir, "game-select/esdecompanion-game-select.sh"),
            File(scriptsDir, "system-select/esdecompanion-system-select.sh"),
            File(scriptsDir, "game-start/esdecompanion-game-start.sh"),
            File(scriptsDir, "game-end/esdecompanion-game-end.sh"),
            File(scriptsDir, "screensaver-start/esdecompanion-screensaver-start.sh"),
            File(scriptsDir, "screensaver-end/esdecompanion-screensaver-end.sh"),
            File(scriptsDir, "screensaver-game-select/esdecompanion-screensaver-game-select.sh")
        )
        return scriptFiles.all { it.exists() }
    }
}
