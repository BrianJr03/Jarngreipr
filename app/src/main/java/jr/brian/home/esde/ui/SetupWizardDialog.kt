package jr.brian.home.esde.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.esde.setup.SetupStep
import jr.brian.home.esde.setup.WarningType

@Composable
fun SetupWizardDialog(
    currentStep: SetupStep,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onSelectScriptsFolder: () -> Unit,
    onSelectMediaFolder: () -> Unit,
    onUseDefaultScriptsPath: () -> Unit,
    onUseDefaultMediaPath: () -> Unit,
    onCreateScripts: () -> Unit,
    onSkipScripts: () -> Unit,
    onGrantPermission: () -> Unit,
    onWarningContinue: () -> Unit,
    onWarningChooseAgain: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getStepTitle(currentStep),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.esde_setup_close),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        is SetupStep.Welcome -> WelcomeContent()
                        is SetupStep.RequestPermissions -> RequestPermissionsContent()
                        is SetupStep.PermissionsGranted -> PermissionsGrantedContent()
                        is SetupStep.SelectScriptsFolder -> SelectScriptsFolderContent()
                        is SetupStep.CreateScripts -> CreateScriptsContent()
                        is SetupStep.SelectMediaFolder -> SelectMediaFolderContent()
                        is SetupStep.EnableScriptsInESDE -> EnableScriptsContent()
                        is SetupStep.Complete -> CompleteContent()
                        is SetupStep.Warning -> WarningContent(currentStep.type, currentStep.path)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (currentStep) {
                    is SetupStep.Welcome -> {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.esde_setup_lets_do_it))
                        }
                    }

                    is SetupStep.RequestPermissions -> {
                        Button(
                            onClick = onGrantPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.esde_setup_grant_permission))
                        }
                    }

                    is SetupStep.PermissionsGranted -> {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.esde_setup_continue))
                        }
                    }

                    is SetupStep.SelectScriptsFolder -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onUseDefaultScriptsPath,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(stringResource(R.string.esde_setup_use_default))
                            }
                            Button(
                                onClick = onSelectScriptsFolder,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6200EE
                                    )
                                )
                            ) {
                                Text(stringResource(R.string.esde_setup_select_folder))
                            }
                        }
                    }

                    is SetupStep.CreateScripts -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSkipScripts,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(stringResource(R.string.esde_setup_skip))
                            }
                            Button(
                                onClick = onCreateScripts,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6200EE
                                    )
                                )
                            ) {
                                Text(stringResource(R.string.esde_setup_create_scripts))
                            }
                        }
                    }

                    is SetupStep.SelectMediaFolder -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onUseDefaultMediaPath,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(stringResource(R.string.esde_setup_use_default))
                            }
                            Button(
                                onClick = onSelectMediaFolder,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6200EE
                                    )
                                )
                            ) {
                                Text(stringResource(R.string.esde_setup_select_folder))
                            }
                        }
                    }

                    is SetupStep.EnableScriptsInESDE -> {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.esde_setup_scripts_enabled))
                        }
                    }

                    is SetupStep.Complete -> {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text(stringResource(R.string.esde_setup_continue))
                        }
                    }

                    is SetupStep.Warning -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onWarningChooseAgain,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(stringResource(R.string.esde_setup_choose_again))
                            }
                            Button(
                                onClick = onWarningContinue,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF6200EE
                                    )
                                )
                            ) {
                                Text(stringResource(R.string.esde_setup_continue_anyway))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent() {
    Text(
        text = stringResource(R.string.esde_setup_welcome_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun RequestPermissionsContent() {
    Text(
        text = stringResource(R.string.esde_setup_permissions_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun PermissionsGrantedContent() {
    Text(
        text = stringResource(R.string.esde_setup_permissions_granted_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun SelectScriptsFolderContent() {
    Text(
        text = stringResource(R.string.esde_setup_scripts_folder_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun CreateScriptsContent() {
    Text(
        text = stringResource(R.string.esde_setup_create_scripts_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun SelectMediaFolderContent() {
    Text(
        text = stringResource(R.string.esde_setup_media_folder_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun EnableScriptsContent() {
    Text(
        text = stringResource(R.string.esde_setup_enable_scripts_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun CompleteContent() {
    Text(
        text = stringResource(R.string.esde_setup_complete_content),
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun WarningContent(type: WarningType, path: String) {
    val message = when (type) {
        WarningType.NonStandardScriptsPath -> stringResource(
            R.string.esde_setup_warning_scripts_path,
            path
        )

        WarningType.NonStandardMediaPath -> stringResource(
            R.string.esde_setup_warning_media_path,
            path
        )

        WarningType.ScriptsMissing -> stringResource(R.string.esde_setup_warning_scripts_missing)
    }

    Text(
        text = message,
        color = Color.White,
        fontSize = 16.sp
    )
}

@Composable
private fun getStepTitle(step: SetupStep): String {
    return when (step) {
        is SetupStep.Welcome -> stringResource(R.string.esde_setup_title_welcome)
        is SetupStep.RequestPermissions -> stringResource(R.string.esde_setup_title_permissions)
        is SetupStep.PermissionsGranted -> stringResource(R.string.esde_setup_title_permissions_granted)
        is SetupStep.SelectScriptsFolder -> stringResource(R.string.esde_setup_title_scripts_folder)
        is SetupStep.CreateScripts -> stringResource(R.string.esde_setup_title_create_scripts)
        is SetupStep.SelectMediaFolder -> stringResource(R.string.esde_setup_title_media_folder)
        is SetupStep.EnableScriptsInESDE -> stringResource(R.string.esde_setup_title_enable_scripts)
        is SetupStep.Complete -> stringResource(R.string.esde_setup_title_complete)
        is SetupStep.Warning -> stringResource(R.string.esde_setup_title_warning)
    }
}
