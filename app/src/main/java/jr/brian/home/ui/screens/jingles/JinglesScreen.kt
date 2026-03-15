package jr.brian.home.ui.screens.jingles

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.AddRepoButton
import jr.brian.home.ui.components.CreateJinglePackButton
import jr.brian.home.ui.components.FolderCard
import jr.brian.home.ui.components.PickFolderButton
import jr.brian.home.ui.components.RepoCard
import jr.brian.home.ui.components.SearchRepoButton
import jr.brian.home.ui.components.dialog.JinglesInfoDialog
import jr.brian.home.ui.components.dialog.JinglesSearchDialog
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.viewmodels.JinglesViewModel

internal const val JINGLES_PREFS = "jingles_prefs"
internal const val KEY_REPOS = "jingle_repos"
internal const val KEY_LOCAL_FOLDERS = "jingle_local_folders"
internal const val KEY_ENABLED = "jingles_enabled"
private const val KEY_INFO_SEEN = "jingles_info_seen"
private const val KEY_PACK_PARENT_URI = "jingles_pack_parent_uri"

@Composable
fun JinglesScreen(
    onNavigateToAddJingle: (String, Boolean, String?, String?) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit = {}
) {
    val viewModel: JinglesViewModel = hiltViewModel()
    val downloadedRepos by viewModel.downloadedRepos.collectAsStateWithLifecycle()
    val downloadingRepo by viewModel.downloadingRepo.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val isRefreshingFolders by viewModel.isRefreshingFolders.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val indexNames by viewModel.indexNames.collectAsStateWithLifecycle()
    val indexCounts by viewModel.indexCounts.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()

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
    var packParentUri by remember { mutableStateOf(prefs.getString(KEY_PACK_PARENT_URI, null)) }
    var showCreatePackDialog by remember { mutableStateOf(false) }
    var selectedExistingPack by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showPackDropdown by remember { mutableStateOf(false) }
    var repoInput by remember { mutableStateOf("") }
    var folderError by remember { mutableStateOf<String?>(null) }
    var isEditingRepo by remember { mutableStateOf(false) }
    var tempRepoInput by remember { mutableStateOf("") }
    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    BackHandler {
        if (isEditingRepo) isEditingRepo = false else onDismiss()
    }

    var reposExpanded by remember { mutableStateOf(false) }
    var foldersExpanded by remember { mutableStateOf(false) }
    val infoDialogState = rememberDialogState<Unit>()
    val searchDialogState = rememberDialogState<Unit>()
    val errorString = stringResource(R.string.jingles_invalid_folder_error)

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean(KEY_INFO_SEEN, false)) infoDialogState.show()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                localFolders = prefs.getStringSet(KEY_LOCAL_FOLDERS, emptySet())?.toList()?.sorted()
                    ?: emptyList()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val uriString = uri.toString()
        if (uriString !in localFolders) {
            val updated = (localFolders + uriString).sorted()
            localFolders = updated
            prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, updated.toSet()) }
        }
        folderError = null
    }

    val createPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val uriString = uri.toString()
        prefs.edit { putString(KEY_PACK_PARENT_URI, uriString) }
        packParentUri = uriString
        selectedExistingPack = null
        onNavigateToAddJingle(uriString, true, null, null)
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
        viewModel.removeDownloadedRepo(repo)
    }

    fun removeLocalFolder(uriString: String) {
        val updated = localFolders.filter { it != uriString }
        localFolders = updated
        prefs.edit { putStringSet(KEY_LOCAL_FOLDERS, updated.toSet()) }
    }

    fun addRepoFromSearch(fullName: String) {
        if (fullName !in repos) {
            val updated = (repos + fullName).sorted()
            repos = updated
            prefs.edit { putStringSet(KEY_REPOS, updated.toSet()) }
            repoInput = ""
        }
    }

    if (showCreatePackDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePackDialog = false },
            containerColor = OledCardColor,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.jingles_create_pack_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.jingles_create_pack_dialog_body),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreatePackDialog = false
                    createPackLauncher.launch(null)
                }) {
                    Text(
                        text = stringResource(R.string.jingles_create_pack_dialog_confirm),
                        color = ThemePrimaryColor
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePackDialog = false }) {
                    Text(
                        text = stringResource(R.string.jingles_create_pack_dialog_cancel),
                        color = Color.Gray
                    )
                }
            }
        )
    }

    if (infoDialogState.isVisible) {
        JinglesInfoDialog(onDismiss = {
            prefs.edit { putBoolean(KEY_INFO_SEEN, true) }
            infoDialogState.dismiss()
        })
    }

    if (searchDialogState.isVisible) {
        JinglesSearchDialog(
            state = searchState,
            existingRepos = repos,
            onAddRepo = ::addRepoFromSearch,
            onDismiss = {
                searchDialogState.dismiss()
                viewModel.clearSearch()
            }
        )
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
                                    onClick = { infoDialogState.show() },
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
                                fontSize = 16.sp,
                                color = Color.Gray,
                                lineHeight = 22.sp
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.jingles_volume),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isMuted) Color.Gray else Color.White
                                )
                                Text(
                                    text = "${(volume * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    color = ThemePrimaryColor.copy(alpha = if (isMuted) 0.4f else 1f)
                                )
                            }
                            Slider(
                                value = volume,
                                onValueChange = { viewModel.setVolume(it) },
                                enabled = !isMuted,
                                colors = SliderDefaults.colors(
                                    thumbColor = ThemePrimaryColor,
                                    activeTrackColor = ThemePrimaryColor,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                    disabledThumbColor = Color.Gray,
                                    disabledActiveTrackColor = Color.Gray.copy(alpha = 0.4f),
                                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                            if (isMuted) {
                                Text(
                                    text = stringResource(R.string.jingles_volume_unmute),
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.jingles_repositories_label),
                            icon = Icons.Default.MusicNote,
                            isExpanded = reposExpanded,
                            onToggle = { reposExpanded = !reposExpanded }
                        ) {
//                            BrowseJinglesButton(onClick = {
//                                searchDialogState.show()
//                                viewModel.browseAllJingles()
//                            })
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            subtleCardGradient(
                                                isFocused = repoInput.isNotBlank()
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (repoInput.isNotBlank()) borderBrush(true) else borderBrush(
                                                false
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            tempRepoInput = repoInput
                                            isEditingRepo = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = repoInput.ifBlank { stringResource(R.string.jingles_repo_placeholder) },
                                        fontSize = 13.sp,
                                        color = if (repoInput.isBlank()) Color.Gray else Color.White,
                                    )
                                }
                                SearchRepoButton(onClick = {
                                    if (repoInput.isNotBlank()) {
                                        searchDialogState.show()
                                        viewModel.searchRepo(repoInput.trim())
                                    }
                                })
                                AddRepoButton(onClick = ::addRepo)
                            }
                            repos.forEach { repo ->
                                val isDownloaded = repo in downloadedRepos
                                RepoCard(
                                    repo = repo,
                                    jingleName = indexNames[repo],
                                    jingleCount = indexCounts[repo],
                                    isDownloaded = isDownloaded,
                                    isDownloading = downloadingRepo == repo,
                                    downloadProgress = downloadProgress.coerceIn(0f, 1f),
                                    downloadedFileCount = if (isDownloaded) viewModel.getDownloadedFileCount(
                                        repo
                                    ) else null,
                                    onRemove = {
                                        if (downloadingRepo == repo) viewModel.cancelDownload()
                                        removeRepo(repo)
                                    },
                                    onDownload = { viewModel.downloadRepo(repo) },
                                    onStopDownload = { viewModel.cancelDownload() },
                                    onFetchSizeBytes = {
                                        if (isDownloaded) viewModel.getDownloadedSizeBytes(repo)
                                        else viewModel.fetchRepoSizeBytes(repo)
                                    }
                                )
                            }
                        }
                    }

                    item {
                        CollapsibleSettingsSection(
                            title = stringResource(R.string.jingles_local_folders_label),
                            icon = Icons.Default.Folder,
                            isExpanded = foldersExpanded,
                            onToggle = { foldersExpanded = !foldersExpanded }
                        ) {
                            Text(
                                text = stringResource(R.string.jingles_local_folder_description),
                                fontSize = 15.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                            val localPackOptions = remember(indexNames, localFolders) {
                                localFolders.map { path ->
                                    val name = indexNames[path]?.takeIf { it.isNotBlank() }
                                        ?: path.substringAfterLast("/")
                                    name to path
                                }
                            }
                            val packButtonLabel = if (selectedExistingPack != null)
                                stringResource(R.string.jingles_update_pack_button)
                            else
                                stringResource(R.string.jingles_create_pack_button)

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max)
                            ) {
                                if (localPackOptions.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .width(46.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    subtleCardGradient(showPackDropdown),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    width = if (showPackDropdown) 2.dp else 1.dp,
                                                    brush = borderBrush(showPackDropdown),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { showPackDropdown = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.jingles_add_pack_select_existing),
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showPackDropdown,
                                            onDismissRequest = { showPackDropdown = false },
                                            containerColor = OledCardColor
                                        ) {
                                            localPackOptions.forEach { (name, path) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            name,
                                                            color = Color.White,
                                                            fontSize = 14.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedExistingPack = name to path
                                                        showPackDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                CreateJinglePackButton(
                                    label = packButtonLabel,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        val existing = selectedExistingPack
                                        if (existing != null) {
                                            onNavigateToAddJingle(
                                                existing.second,
                                                true,
                                                existing.second,
                                                existing.first
                                            )
                                        } else {
                                            val saved = packParentUri
                                            val hasPermission =
                                                saved != null && context.contentResolver
                                                    .persistedUriPermissions.any { it.uri.toString() == saved && it.isReadPermission }
                                            if (saved != null && hasPermission) {
                                                onNavigateToAddJingle(saved, true, null, null)
                                            } else {
                                                showCreatePackDialog = true
                                            }
                                        }
                                    }
                                )
                                PickFolderButton(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = { folderPickerLauncher.launch(null) }
                                )
                            }
                            if (selectedExistingPack != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.jingles_add_pack_updating),
                                        fontSize = 12.sp,
                                        color = ThemePrimaryColor
                                    )
                                    Text(
                                        text = stringResource(R.string.jingles_create_pack_change),
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.clickable {
                                            selectedExistingPack = null
                                        }
                                    )
                                }
                            } else if (packParentUri != null) {
                                val displayName = remember(packParentUri) {
                                    val s = packParentUri ?: ""
                                    if (s.startsWith("content://")) {
                                        runCatching {
                                            DocumentFile.fromTreeUri(context, s.toUri())?.name
                                        }.getOrNull() ?: s.substringAfterLast("/")
                                    } else {
                                        s.substringAfterLast("/")
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.jingles_create_pack_parent,
                                            displayName
                                        ),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = stringResource(R.string.jingles_create_pack_change),
                                        fontSize = 12.sp,
                                        color = ThemePrimaryColor,
                                        modifier = Modifier.clickable {
                                            createPackLauncher.launch(
                                                null
                                            )
                                        }
                                    )
                                }
                            }
                            folderError?.let {
                                Text(
                                    text = it,
                                    fontSize = 14.sp,
                                    color = Color.Red.copy(alpha = 0.85f)
                                )
                            }
                            localFolders.forEach { uriString ->
                                FolderCard(
                                    uriString = uriString,
                                    jingleName = indexNames[uriString],
                                    jingleCount = indexCounts[uriString],
                                    isRefreshing = isRefreshingFolders,
                                    onRefresh = { viewModel.refreshLocalFolders() },
                                    onRemove = { removeLocalFolder(uriString) },
                                    onFetchSizeBytes = { viewModel.getLocalFolderSizeBytes(uriString) }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isEditingRepo,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                JinglesKeyboardOverlay(
                    fieldLabel = stringResource(R.string.jingles_add_repo_field_label),
                    tempText = tempRepoInput,
                    keyboardFocusRequesters = keyboardFocusRequesters,
                    onTextChange = { tempRepoInput = it },
                    onCancel = { isEditingRepo = false },
                    onDone = {
                        repoInput = tempRepoInput
                        isEditingRepo = false
                    }
                )
            }
        }
    }
}
