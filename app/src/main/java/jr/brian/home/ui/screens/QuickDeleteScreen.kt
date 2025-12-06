package jr.brian.home.ui.screens

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.state.DeleteResult
import jr.brian.home.ui.components.quick_delete.ActionButton
import jr.brian.home.ui.components.quick_delete.AddFolderButton
import jr.brian.home.ui.components.quick_delete.DeleteConfirmationDialog
import jr.brian.home.ui.components.quick_delete.FileExtensionCheckboxItem
import jr.brian.home.ui.components.quick_delete.FolderPathItem
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.viewmodels.QuickDeleteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDeleteScreen(
    viewModel: QuickDeleteViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onDismiss)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val documentFile = DocumentFile.fromTreeUri(context, it)
            documentFile?.let { docFile ->
                val folderPath = docFile.uri.toString()
                viewModel.addFolderPath(folderPath)
            }
        }
    }

    uiState.deleteResult?.let { result ->
        LaunchedEffect(result) {
            val message = when (result) {
                is DeleteResult.Success -> {
                    "Successfully deleted ${result.deletedCount} file(s)"
                }

                is DeleteResult.Failure -> {
                    result.message
                }
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearDeleteResult()
        }
    }

    uiState.scanError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearScanError()
        }
    }

    if (uiState.showDeleteConfirmation) {
        val selectedExtensions = uiState.fileExtensions.filter {
            it.extension in uiState.selectedExtensions
        }
        val totalFiles = selectedExtensions.sumOf { it.fileCount }

        DeleteConfirmationDialog(
            fileCount = totalFiles,
            extensionCount = uiState.selectedExtensions.size,
            onConfirm = {
                viewModel.deleteSelectedFiles()
            },
            onDismiss = {
                viewModel.hideDeleteConfirmation()
            }
        )
    }

    val nestedScrollConnection = rememberNestedScrollInteropConnection()

    Scaffold(
        containerColor = OledBackgroundColor,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = stringResource(R.string.clean_folders_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(8f),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            onClick = onDismiss,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clean_folders_close),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.clean_folders_description),
                    color = ThemeSecondaryColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    AddFolderButton(
                        onClick = {
                            folderPickerLauncher.launch(null)
                        }
                    )
                }

                if (uiState.folderPaths.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.clean_folders_selected_folders),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(uiState.folderPaths) { path ->
                        FolderPathItem(
                            path = path,
                            onRemove = {
                                viewModel.removeFolderPath(path)
                            },
                            onClick = {
                                openFolderInFileManager(context, path)
                            }
                        )
                    }

                    item {
                        ActionButton(
                            text = if (uiState.isScanning) {
                                stringResource(R.string.clean_folders_scanning)
                            } else {
                                stringResource(R.string.clean_folders_scan)
                            },
                            icon = Icons.Default.Search,
                            onClick = {
                                viewModel.scanFolders()
                            },
                            enabled = !uiState.isScanning,
                            isLoading = uiState.isScanning,
                            isPrimary = true
                        )
                    }
                } else {
                    item {
                        EmptyFoldersState()
                    }
                }

                if (uiState.fileExtensions.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = ThemePrimaryColor.copy(alpha = 0.3f)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.clean_folders_file_extensions,
                                    uiState.fileExtensions.size
                                ),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ActionButton(
                                    text = stringResource(R.string.clean_folders_select_all),
                                    icon = Icons.Default.FolderOpen,
                                    onClick = { viewModel.selectAllExtensions() },
                                    modifier = Modifier.weight(1f),
                                    isPrimary = false,
                                    isCompact = true
                                )

                                ActionButton(
                                    text = stringResource(R.string.clean_folders_deselect_all),
                                    icon = Icons.Default.Delete,
                                    onClick = { viewModel.deselectAllExtensions() },
                                    modifier = Modifier.weight(1f),
                                    isPrimary = false,
                                    isCompact = true
                                )
                            }
                        }
                    }

                    items(uiState.fileExtensions) { extensionInfo ->
                        FileExtensionCheckboxItem(
                            fileExtensionInfo = extensionInfo,
                            isSelected = extensionInfo.extension in uiState.selectedExtensions,
                            onToggle = {
                                viewModel.toggleExtensionSelection(extensionInfo.extension)
                            }
                        )
                    }

                    item {
                        AnimatedVisibility(
                            visible = uiState.selectedExtensions.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val selectedExtensions = uiState.fileExtensions.filter {
                                it.extension in uiState.selectedExtensions
                            }
                            val totalSize = selectedExtensions.sumOf { it.totalSize }
                            val totalFiles = selectedExtensions.sumOf { it.fileCount }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.clean_folders_total_size,
                                        formatFileSize(totalSize)
                                    ) + stringResource(
                                        R.string.clean_folders_files_separator,
                                        totalFiles
                                    ),
                                    color = ThemeSecondaryColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                ActionButton(
                                    text = stringResource(R.string.clean_folders_delete_selected),
                                    icon = Icons.Default.DeleteSweep,
                                    onClick = {
                                        viewModel.showDeleteConfirmation()
                                    },
                                    enabled = !uiState.isDeleting,
                                    isLoading = uiState.isDeleting,
                                    isPrimary = true
                                )
                            }
                        }
                    }
                } else if (uiState.folderPaths.isNotEmpty() && !uiState.isScanning) {
                    item {
                        Text(
                            text = stringResource(R.string.clean_folders_no_extensions),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFoldersState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.clean_folders_no_paths),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.clean_folders_no_paths_description),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun openFolderInFileManager(
    context: Context,
    uriString: String
) {
    try {
        val uri = Uri.parse(uriString)
        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = 0

        try {
            val browseIntent = Intent("android.provider.action.BROWSE").apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            context.startActivity(browseIntent, options.toBundle())
        } catch (_: Exception) {
            try {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    type = "vnd.android.document/directory"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(viewIntent, options.toBundle())
            } catch (_: Exception) {
                try {
                    val documentsUiIntent = context.packageManager.getLaunchIntentForPackage(
                        "com.android.documentsui"
                    )?.apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    if (documentsUiIntent != null) {
                        context.startActivity(documentsUiIntent, options.toBundle())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
