package jr.brian.home.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.components.dialog.JinglesInfoDialog
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.viewmodels.JinglesViewModel

internal const val JINGLES_PREFS = "jingles_prefs"
internal const val KEY_REPOS = "jingle_repos"
internal const val KEY_LOCAL_FOLDERS = "jingle_local_folders"
internal const val KEY_ENABLED = "jingles_enabled"
private const val KEY_INFO_SEEN = "jingles_info_seen"

@Composable
fun JinglesScreen(
    onDismiss: () -> Unit = {}
) {
    BackHandler(onBack = onDismiss)

    val viewModel: JinglesViewModel = hiltViewModel()
    val downloadedRepos by viewModel.downloadedRepos.collectAsStateWithLifecycle()
    val downloadingRepo by viewModel.downloadingRepo.collectAsStateWithLifecycle()
    val isRefreshingFolders by viewModel.isRefreshingFolders.collectAsStateWithLifecycle()
    val indexNames by viewModel.indexNames.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences(JINGLES_PREFS, 0) }

    var isEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_ENABLED, true)) }
    var repos by remember {
        mutableStateOf<List<String>>(
            prefs.getStringSet(KEY_REPOS, emptySet())?.toList()?.sorted() ?: emptyList()
        )
    }
    var localFolders by remember {
        mutableStateOf<List<String>>(
            prefs.getStringSet(KEY_LOCAL_FOLDERS, emptySet())?.toList()?.sorted() ?: emptyList()
        )
    }
    var repoInput by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }
    var showInfoDialog by remember { mutableStateOf(!prefs.getBoolean(KEY_INFO_SEEN, false)) }
    val errorString = stringResource(R.string.jingles_invalid_folder_error)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val folder = DocumentFile.fromTreeUri(context, uri)
        val hasIndexJson = folder?.findFile("index.json") != null
        val hasJinglesFolder = folder?.findFile("jingles")?.isDirectory == true
        if (!hasIndexJson || !hasJinglesFolder) {
            folderError = errorString
            return@rememberLauncherForActivityResult
        }
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val uriString = uri.toString()
        if (uriString !in localFolders) {
            val updated = (localFolders + uriString).sorted()
            localFolders = updated
            prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, updated.toSet()) }
        }
        folderError = null
    }

    fun addRepo() {
        val trimmed = repoInput.trim()
        if (trimmed.isNotEmpty() && trimmed !in repos) {
            val updated = (repos + trimmed).sorted()
            repos = updated
            prefs.edit { putStringSet(KEY_REPOS, updated.toSet()) }
            repoInput = ""
            focusManager.clearFocus()
        }
    }

    fun removeRepo(repo: String) {
        val updated = repos.filter { it != repo }
        repos = updated
        prefs.edit { putStringSet(KEY_REPOS, updated.toSet()) }
    }

    fun removeLocalFolder(uriString: String) {
        val updated = localFolders.filter { it != uriString }
        localFolders = updated
        prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, updated.toSet()) }
    }

    if (showInfoDialog) {
        JinglesInfoDialog(onDismiss = {
            prefs.edit { putBoolean(KEY_INFO_SEEN, true) }
            showInfoDialog = false
        })
    }

    Scaffold(containerColor = OledBackgroundColor) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBackgroundColor)
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onDismiss)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    ThemePrimaryColor.copy(alpha = 0.25f),
                                                    ThemeSecondaryColor.copy(alpha = 0.2f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = ThemePrimaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.jingles_screen_title),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { showInfoDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(R.string.jingles_info_button_description),
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.jingles_screen_subtitle),
                                fontSize = 14.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    item {
                        ToggleSetting(
                            title = stringResource(R.string.jingles_enable_title),
                            description = stringResource(R.string.jingles_enable_description),
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                prefs.edit { putBoolean(KEY_ENABLED, it) }
                            }
                        )
                    }

                    item {
                        SectionHeader(text = stringResource(R.string.jingles_repositories_label))
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = repoInput,
                                onValueChange = { repoInput = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.jingles_repo_placeholder),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { addRepo() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = ThemePrimaryColor,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = ThemePrimaryColor,
                                    focusedLabelColor = ThemePrimaryColor,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            AddRepoButton(onClick = ::addRepo)
                        }
                    }

                    items(repos, key = { it }) { repo ->
                        RepoCard(
                            repo = repo,
                            jingleName = indexNames[repo],
                            isDownloaded = repo in downloadedRepos,
                            isDownloading = downloadingRepo == repo,
                            onRemove = { removeRepo(repo) },
                            onDownload = { viewModel.downloadRepo(repo) }
                        )
                    }

                    item {
                        SectionHeader(text = stringResource(R.string.jingles_local_folders_label))
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.jingles_local_folder_description),
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp
                            )
                            PickFolderButton(onClick = { folderPickerLauncher.launch(null) })
                            if (folderError != null) {
                                Text(
                                    text = folderError!!,
                                    fontSize = 13.sp,
                                    color = Color.Red.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    items(localFolders, key = { it }) { uriString ->
                        FolderCard(
                            uriString = uriString,
                            jingleName = indexNames[uriString],
                            isRefreshing = isRefreshingFolders,
                            onRefresh = { viewModel.refreshLocalFolders() },
                            onRemove = { removeLocalFolder(uriString) }
                        )
                    }
                }
            }
        }
    }
}
