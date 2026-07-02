package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import jr.brian.home.ui.util.PRIMARY_DISPLAY_ID
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.ui.components.QwertyKeyboard
import androidx.compose.ui.graphics.lerp
import jr.brian.home.data.LocalJinglesManager
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun RomSearchScreen(
    onDismiss: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: RomSearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val jinglesManager = LocalJinglesManager.current
    val prefsManager = LocalESDEPreferencesManager.current
    val prefsState by prefsManager.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val focusedGame by viewModel.focusedGame.collectAsStateWithLifecycle()
    // val hintAndKbVisible by viewModel.hintAndKbVisible.collectAsStateWithLifecycle()

    var showSpecialCharRow by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCommandsDialog by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }

    val fromFrontend = remember { viewModel.stateHolder.openedFromFrontend.value }

    LaunchedEffect(Unit) {
        jinglesManager.stop()
        viewModel.loadGames()
        launchResultsActivity(context, fromFrontend = fromFrontend)
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
            viewModel.resetHintAndKbVisibility()
            viewModel.stateHolder.openedFromFrontend.value = false
        }
    }

    BackHandler {
        when {
            showSettings -> showSettings = false
            query.isNotEmpty() -> viewModel.clearState()
            else -> {
                viewModel.dismiss()
                onDismiss()
            }
        }
    }

    if (showCommandsDialog) {
        SearchCommandsDialog(onDismiss = { showCommandsDialog = false })
    }

    Surface(
        color = if (prefsState.romSearchBlackBackground) Color.Black else OledBackgroundColor,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = true) {
                    RomSearchControlHints {
                        showCommandsDialog = true
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val game = focusedGame
                    if (game?.marqueeImagePath != null) {
                        MarqueeDisplay(game = game)
                    } else if (game != null) {
                        AnimatedGameTitle(name = game.name)
                    }
                }

                AnimatedVisibility(visible = true) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        QwertyKeyboard(
                            searchQuery = query,
                            onQueryChange = { viewModel.updateQuery(it) },
                            keyboardFocusRequesters = keyboardFocusRequesters,
                            showSpecialCharRow = showSpecialCharRow,
                            showFlipLayoutButton = false,
                            showVolControl = true,
                            showAtKey = true,
                            showSettings = true,
                            showController = false,
                            onOpenRomSearchSettings = { showSettings = true },
                            onNavigateToSearch = {
                                viewModel.dismiss()
                                onNavigateToSearch()
                            },
                            onAtClick = { viewModel.updateQuery("$query@") },
                            onReopenResults = { launchResultsActivity(context, fromFrontend = fromFrontend) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it }
            ) {
                RomSearchSettingsScreen(onBack = { showSettings = false })
            }
        }
    }
}

@Composable
private fun RomSearchControlHints(
    onInfoClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(4.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = Icons.Default.Keyboard,
//                contentDescription = null,
//                tint = ThemePrimaryColor.copy(alpha = 0.5f),
//                modifier = Modifier.size(16.dp)
//            )
//            Text(
//                text = stringResource(R.string.rom_search_hint_keyboard),
//                color = ThemePrimaryColor.copy(alpha = 0.5f),
//                fontSize = 14.sp,
//                fontWeight = FontWeight.Medium
//            )
//        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rom_search_hint_details),
                color = ThemePrimaryColor.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Search commands",
                tint = ThemePrimaryColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onInfoClick() }
            )
        }
    }
}

@Composable
private fun SearchCommandsDialog(onDismiss: () -> Unit) {
    val commands = romSearchCommands()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = jr.brian.home.ui.theme.OledCardColor,
        title = {
            Text(
                text = stringResource(R.string.rom_search_commands),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                commands.forEachIndexed { index, (command, description) ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = command,
                            color = ThemePrimaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    if (index < commands.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                        HorizontalDivider(
                            color = Color.White.copy(
                                alpha = 0.07f
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.whats_new_got_it),
                    color = ThemePrimaryColor
                )
            }
        }
    )
}

@Composable
internal fun AnimatedGameTitle(
    name: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val transition = rememberInfiniteTransition(label = "titleColor")
    val color1 by transition.animateColor(
        initialValue = ThemePrimaryColor,
        targetValue = ThemeAccentColor,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleColor1"
    )
    val color2 by transition.animateColor(
        initialValue = ThemeAccentColor,
        targetValue = ThemeSecondaryColor,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleColor2"
    )
    Text(
        text = name,
        color = lerp(color1, color2, 0.5f),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
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

private fun launchResultsActivity(context: Context, fromFrontend: Boolean) {
    val intent = Intent(context, RomSearchResultsActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        putExtra(RomSearchResultsActivity.EXTRA_FROM_FRONTEND, fromFrontend)
    }
    val options = ActivityOptions.makeBasic()
    options.launchDisplayId = PRIMARY_DISPLAY_ID
    context.startActivity(intent, options.toBundle())
}
