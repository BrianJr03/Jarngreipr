package jr.brian.jarnfe.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.utilities.UtilityConfirmDialog
import jr.brian.jarnfe.utilities.UtilityNote
import kotlinx.coroutines.launch

private val ResultsMaxHeight = 280.dp

private sealed interface RepoSheet {
    data object Add : RepoSheet
    data object Search : RepoSheet
    data class Results(val items: List<JarnJingleSearchResult>) : RepoSheet
    data class Actions(val slug: String) : RepoSheet
    data class ConfirmRemove(val slug: String) : RepoSheet
}

@Composable
internal fun JarnJingleReposPage(data: JarnAudioData, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val reposFlow = remember(data) { data.repos }
    val repos by reposFlow.collectAsState(initial = emptyList())
    val download by data.download.collectAsState()
    var sheet by remember { mutableStateOf<RepoSheet?>(null) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val close = { sheet = null }
    val search: (String) -> Unit = { query ->
        close()
        if (query.isNotBlank()) {
            searching = true
            scope.launch {
                val found = data.searchRepos(query)
                searching = false
                if (found == null) {
                    JarnNotices.show(R.string.jarnfe_audio_search_failed)
                } else {
                    sheet = RepoSheet.Results(found)
                }
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        AudioPage(stringResource(R.string.jarnfe_audio_repos), onBack) {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_audio_repo_add),
                onClick = { sheet = RepoSheet.Add },
                modifier = Modifier.fillMaxWidth(),
                supporting = stringResource(R.string.jarnfe_audio_repo_add_hint),
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_audio_repo_search),
                onClick = { sheet = RepoSheet.Search },
                modifier = Modifier.fillMaxWidth(),
                enabled = !searching,
                focusableWhenDisabled = true,
                supporting = stringResource(
                    if (searching) R.string.jarnfe_audio_repo_searching else R.string.jarnfe_audio_repo_search_hint,
                ),
            )
            if (repos.isEmpty()) UtilityNote(stringResource(R.string.jarnfe_audio_repo_empty))
            repos.forEach { repo ->
                RepoRow(repo, download?.takeIf { it.slug == repo.slug }) { sheet = RepoSheet.Actions(repo.slug) }
            }
        }
        RepoSheetHost(sheet, repos, download, data, search, { sheet = it }, close)
    }
}

@Composable
private fun RepoRow(repo: JarnJingleRepo, download: JarnJingleDownload?, onClick: () -> Unit) {
    val status = when {
        download != null -> stringResource(R.string.jarnfe_audio_repo_downloading, (download.progress * 100).toInt())
        repo.downloaded -> stringResource(R.string.jarnfe_audio_repo_offline)
        repo.entryCount != null -> stringResource(R.string.jarnfe_audio_repo_online, repo.entryCount)
        else -> stringResource(R.string.jarnfe_audio_repo_unreachable)
    }
    JarnNavigateRow(
        label = repo.name ?: repo.slug,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_audio_repo_supporting, repo.slug, status),
    )
}

@Composable
private fun RepoSheetHost(
    sheet: RepoSheet?,
    repos: List<JarnJingleRepo>,
    download: JarnJingleDownload?,
    data: JarnAudioData,
    onSearch: (String) -> Unit,
    onSheet: (RepoSheet) -> Unit,
    close: () -> Unit,
) {
    val add: (String) -> Unit = { raw ->
        close()
        addRepoWithNotice(data, raw)
    }
    when (sheet) {
        null -> Unit
        RepoSheet.Add -> AudioKeyboardSheet(stringResource(R.string.jarnfe_audio_repo_add_label), add, close)
        RepoSheet.Search -> AudioKeyboardSheet(stringResource(R.string.jarnfe_audio_repo_search_label), onSearch, close)
        is RepoSheet.Results -> RepoResultsSheet(sheet.items, repos, add, close)
        is RepoSheet.Actions -> repos.firstOrNull { it.slug == sheet.slug }?.let { repo ->
            RepoActionsSheet(repo, download, data, onSheet, close)
        }
        is RepoSheet.ConfirmRemove -> UtilityConfirmDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_audio_repo_remove_title),
            message = stringResource(R.string.jarnfe_audio_repo_remove_message, sheet.slug),
            confirmLabel = stringResource(R.string.jarnfe_audio_remove),
            onConfirm = {
                data.removeRepo(sheet.slug)
                close()
            },
            onDismiss = close,
        )
    }
}

private fun addRepoWithNotice(data: JarnAudioData, raw: String) {
    if (raw.isBlank()) return
    val slug = jarnJingleRepoSlug(raw)
    JarnNotices.show(
        when {
            slug == null -> R.string.jarnfe_audio_repo_invalid
            data.addRepo(slug) -> R.string.jarnfe_audio_repo_added
            else -> R.string.jarnfe_audio_repo_duplicate
        },
    )
}

@Composable
private fun RepoResultsSheet(
    results: List<JarnJingleSearchResult>,
    repos: List<JarnJingleRepo>,
    onAdd: (String) -> Unit,
    close: () -> Unit,
) {
    val first = remember { FocusRequester() }
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_audio_repo_results),
        initialFocus = first.takeIf { results.isNotEmpty() },
        onClose = close,
    ) {
        if (results.isEmpty()) UtilityNote(stringResource(R.string.jarnfe_audio_repo_no_results))
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = ResultsMaxHeight).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            results.forEachIndexed { index, result ->
                val added = repos.any { it.slug.equals(result.slug, ignoreCase = true) }
                JarnNavigateRow(
                    label = result.slug,
                    onClick = { onAdd(result.slug) },
                    modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(first) else Modifier),
                    enabled = !added,
                    focusableWhenDisabled = true,
                    supporting = if (added) {
                        stringResource(R.string.jarnfe_audio_repo_already_added)
                    } else {
                        result.description?.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.jarnfe_audio_repo_stars, result.stars)
                    },
                )
            }
        }
    }
}

@Composable
private fun RepoActionsSheet(
    repo: JarnJingleRepo,
    download: JarnJingleDownload?,
    data: JarnAudioData,
    onSheet: (RepoSheet) -> Unit,
    close: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val isThisDownloading = download?.slug == repo.slug
    val canDownload = jarnJingleRepoSlug(repo.slug) != null && download == null
    CanvasModalSheet(title = repo.name ?: repo.slug, initialFocus = first, onClose = close) {
        JarnNavigateRow(
            label = stringResource(
                when {
                    isThisDownloading -> R.string.jarnfe_audio_repo_stop
                    repo.downloaded -> R.string.jarnfe_audio_repo_update
                    else -> R.string.jarnfe_audio_repo_download
                },
            ),
            onClick = {
                if (isThisDownloading) data.cancelDownload() else data.startDownload(repo.slug, ::noticeForDownload)
                close()
            },
            modifier = Modifier.fillMaxWidth().focusRequester(first),
            enabled = isThisDownloading || canDownload,
            focusableWhenDisabled = true,
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_audio_remove),
            onClick = { onSheet(RepoSheet.ConfirmRemove(repo.slug)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun noticeForDownload(result: JarnJingleDownloadResult) {
    JarnNotices.show(
        when (result) {
            JarnJingleDownloadResult.Done -> R.string.jarnfe_audio_repo_download_done
            JarnJingleDownloadResult.Rejected -> R.string.jarnfe_audio_repo_download_rejected
            JarnJingleDownloadResult.Failed -> R.string.jarnfe_audio_repo_download_failed
        },
    )
}
