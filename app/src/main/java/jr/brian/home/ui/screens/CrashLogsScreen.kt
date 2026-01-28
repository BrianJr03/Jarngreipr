package jr.brian.home.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.CrashLog
import jr.brian.home.ui.components.crashlogs.CrashLogDetailView
import jr.brian.home.ui.components.crashlogs.CrashLogsList
import jr.brian.home.ui.components.crashlogs.EmptyCrashLogsView
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.CrashLogger
import kotlinx.coroutines.launch

@Composable
fun CrashLogsScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val crashDetailDialogState = rememberDialogState<CrashLog>()
    val clearDialogState = rememberDialogState<Unit>()
    var crashLogs by remember { mutableStateOf<List<CrashLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        crashLogs = CrashLogger.getCrashLogs()
        isLoading = false
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.crash_logs_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Report Issue button
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(context.getString(R.string.crash_logs_report_issue_url))
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemePrimaryColor.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Report,
                                contentDescription = stringResource(R.string.crash_logs_report_issue),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.crash_logs_report_issue))
                        }

                        // Clear All button (only show if there are crash logs)
                        if (crashLogs.isNotEmpty()) {
                            Button(
                                onClick = { clearDialogState.show() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.3f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.crash_logs_clear_all),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(stringResource(R.string.crash_logs_clear_all))
                            }
                        }
                    }
                }

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ThemePrimaryColor)
                        }
                    }

                    crashLogs.isEmpty() -> {
                        EmptyCrashLogsView()
                    }

                    else -> {
                        CrashLogsList(
                            crashLogs = crashLogs,
                            onCrashClick = crashDetailDialogState::show,
                            onDeleteClick = { crash ->
                                scope.launch {
                                    CrashLogger.deleteCrashLog(crash.fileName)
                                    crashLogs = CrashLogger.getCrashLogs()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (crashDetailDialogState.isVisible && crashDetailDialogState.item != null) {
        CrashLogDetailView(
            crash = crashDetailDialogState.item!!,
            onDismiss = crashDetailDialogState::dismiss
        )
    }

    if (clearDialogState.isVisible) {
        ClearAllCrashLogsDialog(
            crashLogCount = crashLogs.size,
            onDismiss = clearDialogState::dismiss,
            onConfirm = {
                scope.launch {
                    CrashLogger.clearAllCrashLogs()
                    crashLogs = emptyList()
                    clearDialogState.dismiss()
                }
            }
        )
    }
}

@Composable
private fun ClearAllCrashLogsDialog(
    crashLogCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(R.string.crash_logs_clear_all_dialog_title),
                color = Color.White
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.crash_logs_clear_all_dialog_message,
                    crashLogCount
                ),
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text(stringResource(R.string.crash_logs_clear_all_confirm))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text(stringResource(R.string.crash_logs_clear_all_cancel))
            }
        }
    )
}
