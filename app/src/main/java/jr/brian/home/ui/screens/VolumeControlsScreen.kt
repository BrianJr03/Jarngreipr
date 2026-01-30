package jr.brian.home.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.components.DualVolumeControls
import jr.brian.home.ui.components.dialog.VolumeControlsHelpDialog
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.util.rememberDialogState

@Composable
fun VolumeControlsScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val helpDialogState = rememberDialogState<Unit>()
    
    var canWriteSecureSettings by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        canWriteSecureSettings = try {
            Settings.Global.putInt(context.contentResolver, "test_dual_volume_permission", 1)
            Settings.Global.getInt(context.contentResolver, "test_dual_volume_permission") == 1
        } catch (_: Exception) {
            false
        }
    }
    
    BackHandler {
        onDismiss()
    }
    
    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onDismiss)
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.volume_controls_title),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(
                            onClick = { helpDialogState.show() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.volume_controls_help),
                                tint = ThemePrimaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    if (!canWriteSecureSettings) {
                        PermissionWarningCard(
                            packageName = context.packageName,
                            onCheckPermission = {
                                canWriteSecureSettings = try {
                                    Settings.Global.putInt(context.contentResolver, "test_dual_volume_permission", 1)
                                    Settings.Global.getInt(context.contentResolver, "test_dual_volume_permission") == 1
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        DualVolumeControls(isVisible = true)
                    }
                }
            }
            
            if (helpDialogState.isVisible) {
                VolumeControlsHelpDialog(
                    onDismiss = helpDialogState::dismiss
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    packageName: String,
    onCheckPermission: () -> Unit
) {
    val context = LocalContext.current
    val command = stringResource(R.string.volume_controls_command, packageName)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = Color(0xFFFFA500),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.volume_controls_permission_required),
            color = Color(0xFFFFA500),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.volume_controls_command_title),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = command,
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ADB Command", command)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        context,
                        context.getString(R.string.volume_controls_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.volume_controls_copy),
                    tint = ThemePrimaryColor
                )
            }
        }
        
        Button(
            onClick = onCheckPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemePrimaryColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.volume_controls_go_to_settings),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
