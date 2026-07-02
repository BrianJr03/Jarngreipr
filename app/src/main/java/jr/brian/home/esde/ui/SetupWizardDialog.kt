package jr.brian.home.esde.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.model.SetupStep
import jr.brian.home.esde.model.WarningType
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.DimmedBottomSheet
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

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
    DimmedBottomSheet(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
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
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.esde_setup_close),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
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
                        SetupButton(
                            text = stringResource(R.string.esde_setup_start_setup),
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }

                    is SetupStep.RequestPermissions -> {
                        SetupButton(
                            text = stringResource(R.string.esde_setup_grant_permission),
                            onClick = onGrantPermission,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }

                    is SetupStep.PermissionsGranted -> {
                        SetupButton(
                            text = stringResource(R.string.esde_setup_next),
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }

                    is SetupStep.SelectScriptsFolder -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SetupButton(
                                text = stringResource(R.string.esde_setup_use_default),
                                onClick = onUseDefaultScriptsPath,
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                            SetupButton(
                                text = stringResource(R.string.esde_setup_select_folder),
                                onClick = onSelectScriptsFolder,
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                        }
                    }

                    is SetupStep.CreateScripts -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SetupButton(
                                text = stringResource(R.string.esde_setup_skip),
                                onClick = onSkipScripts,
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                            SetupButton(
                                text = stringResource(R.string.esde_setup_create_scripts),
                                onClick = onCreateScripts,
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                        }
                    }

                    is SetupStep.SelectMediaFolder -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SetupButton(
                                text = stringResource(R.string.esde_setup_use_default),
                                onClick = onUseDefaultMediaPath,
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                            SetupButton(
                                text = stringResource(R.string.esde_setup_select_folder),
                                onClick = onSelectMediaFolder,
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                        }
                    }

                    is SetupStep.EnableScriptsInESDE -> {
                        SetupButton(
                            text = stringResource(R.string.esde_setup_scripts_done),
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }

                    is SetupStep.Complete -> {
                        SetupButton(
                            text = stringResource(R.string.esde_setup_next),
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )
                    }

                    is SetupStep.Warning -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SetupButton(
                                text = stringResource(R.string.esde_setup_choose_again),
                                onClick = onWarningChooseAgain,
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                            SetupButton(
                                text = stringResource(R.string.esde_setup_continue_anyway),
                                onClick = onWarningContinue,
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
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

@Composable
private fun SetupButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundBrush = if (isPrimary) {
        cardGradient(isFocused = isFocused, isSelected = true)
    } else {
        cardGradient(isFocused = isFocused)
    }

    val borderBrush = borderBrush(
        isFocused = true,
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.8f),
                ThemeSecondaryColor.copy(alpha = 0.6f)
            )
        } else if (isPrimary) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.4f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.2f)
            )
        }
    )

    Box(
        modifier = modifier
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = backgroundBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
