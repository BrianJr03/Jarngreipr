package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.theme.OledBackgroundColor
import java.io.File

@Composable
fun RomSearchScreen(
    onDismiss: () -> Unit = {},
    viewModel: RomSearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val focusedGame by viewModel.focusedGame.collectAsStateWithLifecycle()
    val keyboardVisible by viewModel.keyboardVisible.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadGames()
        launchResultsActivity(context)
    }

    LaunchedEffect(Unit) {
        viewModel.screenDismissSignal.collect {
            viewModel.dismiss()
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearState()
            viewModel.resetKeyboardVisibility()
        }
    }

    BackHandler {
        if (query.isNotEmpty()) {
            viewModel.clearState()
        } else {
            viewModel.dismiss()
            onDismiss()
        }
    }

    var showSpecialCharRow by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MarqueeDisplay(game = focusedGame)
            }

            AnimatedVisibility(visible = keyboardVisible) {
                QwertyKeyboard(
                    searchQuery = query,
                    onQueryChange = { viewModel.updateQuery(it) },
                    keyboardFocusRequesters = keyboardFocusRequesters,
                    showSpecialCharRow = showSpecialCharRow,
                    showFlipLayoutButton = false,
                    onSpecialCharToggle = { showSpecialCharRow = !showSpecialCharRow },
                    onReopenResults = { launchResultsActivity(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MarqueeDisplay(game: GameInfo?) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current

    AnimatedContent(
        targetState = game?.marqueeImagePath,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "marquee"
    ) { marqueePath ->
        if (marqueePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(marqueePath))
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = game?.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun launchResultsActivity(context: Context) {
    val intent = Intent(context, RomSearchResultsActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
    val options = ActivityOptions.makeBasic()
    options.launchDisplayId = 0
    context.startActivity(intent, options.toBundle())
}
