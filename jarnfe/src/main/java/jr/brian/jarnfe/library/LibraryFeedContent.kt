package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

internal data class LibraryEmptyCopy(@param:StringRes val titleRes: Int, @param:StringRes val messageRes: Int)

@Composable
internal fun <T> LibraryFeedContent(
    feed: JarnLibraryFeed<T>,
    cellMinSize: Dp,
    tileRatio: Float,
    onRetry: () -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val status = feed.status
    when {
        feed.items.isNotEmpty() -> Column(Modifier.fillMaxSize()) {
            if (status == JarnLibraryStatus.Unavailable) LibraryUnavailableBanner(onRetry)
            if (status is JarnLibraryStatus.Loading) LibraryProgressLine(status)
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
        status is JarnLibraryStatus.Loading -> Column(Modifier.fillMaxSize()) {
            LibraryProgressLine(status)
            LibrarySkeletonGrid(cellMinSize, tileRatio, Modifier.weight(1f))
        }
        status == JarnLibraryStatus.Unavailable -> LibraryUnavailableState(onRetry)
        else -> empty()
    }
}

@Composable
private fun LibraryProgressLine(status: JarnLibraryStatus.Loading) {
    val loaded = status.loaded
    val total = status.total
    val text = if (loaded != null && total != null) {
        stringResource(R.string.jarnfe_library_scan_progress, loaded, total)
    } else {
        stringResource(R.string.jarnfe_library_loading)
    }
    JarnText(
        text = text,
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
        modifier = Modifier.padding(horizontal = JarnTheme.space.stageMargin),
        maxLines = 1,
    )
}

@Composable
private fun LibraryUnavailableBanner(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.stageMargin, vertical = JarnTheme.space.xs),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(
            text = stringResource(R.string.jarnfe_library_unavailable_title),
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textMid,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_library_try_again),
            onClick = onRetry,
            variant = JarnButtonVariant.Secondary,
        )
    }
}

@Composable
private fun LibraryUnavailableState(onRetry: () -> Unit) {
    val requester = remember { FocusRequester() }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) { if (!tabsFocus.hasFocus) requester.requestFocus() }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JarnEmptyState(
            title = stringResource(R.string.jarnfe_library_unavailable_title),
            message = stringResource(R.string.jarnfe_library_unavailable_message),
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_library_try_again),
            onClick = onRetry,
            modifier = Modifier.focusRequester(requester),
            variant = JarnButtonVariant.Secondary,
        )
    }
}

@Composable
internal fun LibraryEmptyMessage(copy: LibraryEmptyCopy, actions: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JarnEmptyState(title = stringResource(copy.titleRes), message = stringResource(copy.messageRes))
        actions()
    }
}
