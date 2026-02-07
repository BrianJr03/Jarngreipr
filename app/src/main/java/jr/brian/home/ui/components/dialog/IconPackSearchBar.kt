package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import jr.brian.home.ui.components.VerticalKeyboard

@Composable
fun IconPackSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        VerticalKeyboard(
            searchQuery = searchQuery,
            onQueryChange = onSearchQueryChange,
            showQueryText = true,
            keyboardFocusRequesters = keyboardFocusRequesters,
            onFocusChanged = onFocusChanged,
            onNavigateRight = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
