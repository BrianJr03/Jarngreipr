package jr.brian.home.esde.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import jr.brian.home.esde.data.ScriptManager
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.model.SetupStep
import jr.brian.home.esde.data.SetupWizardManager
import jr.brian.home.esde.model.WarningType
import jr.brian.home.ui.util.DialogState
import java.io.File

@Composable
fun ESDESetupScreen(
    dialogState: DialogState<SetupStep>,
    onDismiss: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { SetupPreferences(context) }
    
    var currentStep by remember { mutableStateOf<SetupStep>(SetupStep.Welcome) }
    val isDialogVisible = dialogState.isVisible
    
    val setupManager = remember {
        SetupWizardManager(
            context = context,
            preferences = preferences,
            onStepChanged = { step -> currentStep = step },
            onWizardComplete = {
                currentStep = SetupStep.Complete
            }
        )
    }
    
    LaunchedEffect(isDialogVisible) {
        if (isDialogVisible) {
            currentStep = SetupStep.Welcome
        }
    }
    
    val scriptsFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                if (setupManager.isValidScriptsPath(path)) {
                    preferences.scriptsPath = path
                    setupManager.continueWizard()
                } else {
                    currentStep = SetupStep.Warning(WarningType.NonStandardScriptsPath, path)
                }
            }
        }
    }
    
    val mediaFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                if (setupManager.isValidMediaPath(path)) {
                    preferences.mediaPath = path
                    setupManager.continueWizard()
                } else {
                    currentStep = SetupStep.Warning(WarningType.NonStandardMediaPath, path)
                }
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (setupManager.hasStoragePermission()) {
            currentStep = SetupStep.PermissionsGranted
        }
    }
    
    if (isDialogVisible) {
        SetupWizardDialog(
            currentStep = currentStep,
            onDismiss = {
                setupManager.cancelWizard()
                dialogState.dismiss()
                onDismiss()
            },
            onContinue = {
                when (currentStep) {
                    is SetupStep.Complete -> {
                        preferences.setupCompleted = true
                        dialogState.dismiss()
                        onSetupComplete()
                    }

                    else -> setupManager.continueWizard()
                }
            },
            onSelectScriptsFolder = {
                scriptsFolderPicker.launch(null)
            },
            onSelectMediaFolder = {
                mediaFolderPicker.launch(null)
            },
            onUseDefaultScriptsPath = {
                preferences.scriptsPath = SetupPreferences.DEFAULT_SCRIPTS_PATH
                setupManager.continueWizard()
            },
            onUseDefaultMediaPath = {
                preferences.mediaPath = SetupPreferences.DEFAULT_MEDIA_PATH
                setupManager.continueWizard()
            },
            onCreateScripts = {
                val scriptsDir = File(preferences.scriptsPath)
                val result = ScriptManager.createAllScripts(scriptsDir)
                if (result.success) {
                    Toast.makeText(context, "Scripts created successfully!", Toast.LENGTH_SHORT)
                        .show()
                    setupManager.continueWizard()
                } else {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            },
            onSkipScripts = {
                if (!setupManager.areScriptsInstalled(preferences.scriptsPath)) {
                    currentStep =
                        SetupStep.Warning(WarningType.ScriptsMissing, preferences.scriptsPath)
                } else {
                    setupManager.continueWizard()
                }
            },
            onGrantPermission = {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${context.packageName}".toUri()
                    permissionLauncher.launch(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    permissionLauncher.launch(intent)
                }
            },
            onWarningContinue = {
                val warning = currentStep as? SetupStep.Warning
                when (warning?.type) {
                    WarningType.NonStandardScriptsPath -> {
                        preferences.scriptsPath = warning.path
                        setupManager.continueWizard()
                    }

                    WarningType.NonStandardMediaPath -> {
                        preferences.mediaPath = warning.path
                        setupManager.continueWizard()
                    }

                    WarningType.ScriptsMissing -> {
                        setupManager.continueWizard()
                    }

                    null -> {}
                }
            },
            onWarningChooseAgain = {
                val warning = currentStep as? SetupStep.Warning
                when (warning?.type) {
                    WarningType.NonStandardScriptsPath -> {
                        currentStep = SetupStep.SelectScriptsFolder
                    }

                    WarningType.NonStandardMediaPath -> {
                        currentStep = SetupStep.SelectMediaFolder
                    }

                    WarningType.ScriptsMissing -> {
                        currentStep = SetupStep.CreateScripts
                    }

                    null -> {}
                }
            }
        )
    }
}

/**
 * Convert content URI to file path
 */
private fun getPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null
    
    return when {
        path.contains("/tree/primary:") -> {
            val relativePath = path.substringAfter("/tree/primary:")
            "/storage/emulated/0/$relativePath"
        }
        path.contains("/tree/") -> {
            // Handle external SD card or other storage
            val storagePart = path.substringAfter("/tree/").substringBefore(":")
            val relativePath = path.substringAfter(":")
            "/storage/$storagePart/$relativePath"
        }
        else -> path
    }
}
