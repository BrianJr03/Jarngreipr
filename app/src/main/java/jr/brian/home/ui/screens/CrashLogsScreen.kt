package jr.brian.home.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.CrashLog
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.CrashLogger
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrashLogsScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var crashLogs by remember { mutableStateOf<List<CrashLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCrash by remember { mutableStateOf<CrashLog?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

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
                                onClick = { showClearDialog = true },
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
                            onCrashClick = { selectedCrash = it },
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

    if (selectedCrash != null) {
        CrashDetailDialog(
            crash = selectedCrash!!,
            onDismiss = { selectedCrash = null },
            onCopy = { content ->
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    context.getString(R.string.crash_logs_title),
                    content
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    context.getString(R.string.crash_logs_copied_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
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
                        crashLogs.size
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            CrashLogger.clearAllCrashLogs()
                            crashLogs = emptyList()
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text(stringResource(R.string.crash_logs_clear_all_confirm))
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text(stringResource(R.string.crash_logs_clear_all_cancel))
                }
            }
        )
    }
}

@Composable
private fun CrashLogsList(
    crashLogs: List<CrashLog>,
    onCrashClick: (CrashLog) -> Unit,
    onDeleteClick: (CrashLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(crashLogs, key = { it.fileName }) { crash ->
            CrashLogCard(
                crash = crash,
                onClick = { onCrashClick(crash) },
                onDeleteClick = { onDeleteClick(crash) }
            )
        }
    }
}

@Composable
private fun CrashLogCard(
    crash: CrashLog,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        } else {
            listOf(
                OledCardLightColor,
                OledCardColor
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor,
                            ThemeSecondaryColor
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.2f),
                            ThemeSecondaryColor.copy(alpha = 0.2f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(crash.timestamp)),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = crash.preview,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = { onDeleteClick() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.crash_logs_delete_description),
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CrashDetailDialog(
    crash: CrashLog,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCardColor,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.crash_logs_detail_title),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onCopy(crash.content) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.crash_logs_detail_copy),
                        tint = ThemePrimaryColor
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                item {
                    Text(
                        text = crash.content,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimaryColor
                )
            ) {
                Text(stringResource(R.string.crash_logs_detail_close))
            }
        }
    )
}

@Composable
private fun EmptyCrashLogsView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = stringResource(R.string.crash_logs_empty_title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.crash_logs_empty_description),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
