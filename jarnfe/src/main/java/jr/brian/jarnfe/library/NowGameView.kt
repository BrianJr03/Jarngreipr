package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.R as CoreR
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.core.ui.library.JarnMetadataPlate
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.launch.GameActionsHost
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.GameSetupRows
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.launch.JarnGameActionsController
import jr.brian.jarnfe.launch.JarnGameFileStatus

private const val MARQUEE_WIDTH_FRACTION = 0.8f
private const val MEDIA_SIZE_FRACTION = 0.84f
private const val SHEET_ALPHA = 0.95f
private const val TITLE_MAX_LINES = 2
private const val DESCRIPTION_MAX_LINES = 8
private val CompactHeight = 350.dp
private val FavoriteSize = 48.dp
private val FavoriteIconSize = 22.dp

private data class NowPosterSizes(val title: Dp, val caseWidth: Dp, val caseHeight: Dp, val peek: Dp)

/**
 * The case is about 10% larger than it first shipped (118 by 150, and 96 by 124 compact). The room
 * comes from the disc's peek and the gap to the text, so the text column keeps its width.
 */
private val RegularPoster = NowPosterSizes(title = 76.dp, caseWidth = 130.dp, caseHeight = 165.dp, peek = 16.dp)
private val CompactPoster = NowPosterSizes(title = 64.dp, caseWidth = 106.dp, caseHeight = 137.dp, peek = 14.dp)
internal const val NOW_CASE_TAG = "now_case"
internal const val NOW_POSTER_TEXT_TAG = "now_poster_text"

/** The selected game's foreground. Call it outside a vertical scroller, since it scrolls itself. */
@Composable
internal fun NowGameView(game: JarnLibraryGame, resume: Boolean = false) {
    val controller = JarnGameActions.controller
    val confirmHint = remember(game.id) { NowConfirmHint() }
    val resumeStatus = if (resume) rememberGameFileStatus(game) else null
    val resumeFocus = remember { FocusRequester() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewport = maxHeight.takeIf { constraints.hasBoundedHeight }
        NowGameScroller(game.id, viewport, showsPicker = controller != null && JarnGameActions.page != GameActionsPage.Actions) {
            if (controller == null) {
                NowGamePage(
                    game = game,
                    viewport = viewport,
                    confirmHint = confirmHint,
                    resume = NowResumeLook(resume, resumeStatus),
                    playRow = { if (resume) NowResumeRow(game, resumeStatus, resumeFocus, null, confirmHint) },
                )
            } else {
                GameActionsHost(game, controller) { playFocus ->
                    JarnInputLayer(
                        enabled = confirmHint.label != null,
                        bindings = { confirmHint.label?.let { hint(JarnAction.Confirm, it) } },
                    )
                    NowGamePage(
                        game = game,
                        viewport = viewport,
                        confirmHint = confirmHint,
                        resume = NowResumeLook(resume, resumeStatus),
                        playRow = {
                            if (resume) {
                                NowResumeRow(game, resumeStatus, playFocus, controller, confirmHint)
                            } else {
                                NowPlayRow(game, controller, playFocus, confirmHint)
                            }
                        },
                        setupRows = { GameSetupRows(game, controller) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NowGameScroller(
    gameId: String,
    viewport: Dp?,
    showsPicker: Boolean,
    content: @Composable () -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(gameId, showsPicker) { scroll.scrollTo(0) }
    val page = @Composable { modifier: Modifier ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(if (viewport != null) Modifier.heightIn(min = viewport) else Modifier)
                .then(if (showsPicker) Modifier.nowPickerSheet() else Modifier),
            verticalArrangement = if (showsPicker) Arrangement.spacedBy(JarnTheme.space.md) else Arrangement.Top,
        ) { content() }
    }
    if (viewport == null) {
        page(Modifier)
    } else {
        JarnRevealScrollTop(scroll, topZone = viewport) { page(Modifier.verticalScroll(scroll)) }
    }
}

@Composable
private fun Modifier.nowPickerSheet(): Modifier = this
    .background(JarnTheme.colors.bg.copy(alpha = SHEET_ALPHA))
    .padding(horizontal = JarnTheme.space.deckMargin, vertical = JarnTheme.space.focusEdge)

@Composable
private fun NowGamePage(
    game: JarnLibraryGame,
    viewport: Dp?,
    confirmHint: NowConfirmHint,
    resume: NowResumeLook = NowResumeLook(),
    playRow: @Composable () -> Unit = {},
    setupRows: (@Composable () -> Unit)? = null,
) {
    val art = rememberNowGameArt(game)
    val marqueeBounds = rememberNowArtBounds(art.marqueePath)
    val coverBounds = rememberNowArtBounds(art.coverPath)
    NowRevealWhen(
        art.isLoaded && (art.marqueePath == null || marqueeBounds != null) &&
            (art.coverPath == null || coverBounds != null),
    )
    val sizes = if (viewport != null && viewport < CompactHeight) CompactPoster else RegularPoster
    NowPoster(game, art, sizes, viewport, resume, playRow)
    NowMoreSheet(game, setupRows, confirmHint)
}

@Composable
private fun NowPoster(
    game: JarnLibraryGame,
    art: NowGameArt,
    sizes: NowPosterSizes,
    viewport: Dp?,
    resume: NowResumeLook,
    playRow: @Composable () -> Unit,
) {
    val artAlpha = missingArtAlpha(resume.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (viewport != null) Modifier.heightIn(min = viewport) else Modifier)
            .padding(
                start = JarnTheme.space.deckMargin,
                end = JarnTheme.space.deckMargin,
                top = JarnTheme.space.md,
                bottom = JarnTheme.space.focusEdge,
            ),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
            if (resume.isResume) NowResumeEyebrow()
            key(game.id) { NowTitleArt(game.title, art, sizes.title, Modifier.alpha(artAlpha)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = JarnTheme.space.sm),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            key(game.id) { NowGameCase(game.title, art, sizes, Modifier.alpha(artAlpha).testTag(NOW_CASE_TAG)) }
            Column(
                modifier = Modifier.weight(1f).testTag(NOW_POSTER_TEXT_TAG),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                if (resume.isResume) {
                    NowResumeFacts(game, resume.status)
                } else {
                    NowFactsPlate(remember(game.metadata) { game.metadata.nowFacts() })
                }
                playRow()
            }
        }
        if (resume.isResume) {
            Box(Modifier.padding(top = JarnTheme.space.md)) {
                NowAlsoRecent(viewport?.let(::nowCoverHeight) ?: nowCoverHeight(Dp.Infinity))
            }
        }
    }
}

/** How the poster reads for the Continue playing card. [status] is null while it is checked. */
private data class NowResumeLook(val isResume: Boolean = false, val status: JarnGameFileStatus? = null)

@Composable
private fun NowTitleArt(title: String, art: NowGameArt, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height)) {
        val marquee = art.marqueePath
        when {
            !art.isLoaded -> Unit
            marquee == null -> NowTitlePlate(title, Modifier.align(Alignment.CenterStart))
            else -> rememberNowArtBounds(marquee)?.let { bounds ->
                if (nowMarqueeFits(bounds)) {
                    NowMarquee(marquee, title)
                } else {
                    NowTitlePlate(title, Modifier.align(Alignment.CenterStart))
                }
            }
        }
    }
}

@Composable
private fun NowMarquee(path: String, title: String) {
    val scrim = JarnTheme.colors.artScrim
    Box(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = title }
            .background(Brush.horizontalGradient(listOf(scrim, Color.Transparent)), JarnTheme.radius.smShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(MARQUEE_WIDTH_FRACTION)
                .fillMaxSize()
                .padding(JarnTheme.space.xs),
        ) {
            val paths = remember(path) { listOf(path) }
            LibraryHeldArt(paths, ContentScale.Fit, Alignment.CenterStart) {
                NowTitlePlate(title, Modifier.align(Alignment.CenterStart))
            }
        }
    }
}

@Composable
private fun NowTitlePlate(title: String, modifier: Modifier = Modifier) {
    JarnText(
        text = title,
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        maxLines = TITLE_MAX_LINES,
        modifier = modifier
            .background(JarnTheme.colors.artScrim, JarnTheme.radius.smShape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.xs),
    )
}

@Composable
private fun NowGameCase(title: String, art: NowGameArt, sizes: NowPosterSizes, modifier: Modifier = Modifier) {
    Box(modifier.size(sizes.caseWidth + sizes.peek, sizes.caseHeight)) {
        art.mediaPath?.let { NowPhysicalMedia(it, sizes, Modifier.align(Alignment.CenterEnd)) }
        val coverSize = nowCaseCoverSize(sizes, nowCoverAspect(rememberNowArtBounds(art.coverPath)))
        NowCover(title, art, Modifier.align(Alignment.BottomStart).size(coverSize.width, coverSize.height))
    }
}

/** What A does on the focused control, for the hint bar. */
private class NowConfirmHint {
    @get:StringRes
    var label by mutableStateOf<Int?>(null)
}

private fun Modifier.nowConfirmHint(hint: NowConfirmHint, @StringRes label: Int): Modifier =
    onFocusChanged { state ->
        when {
            state.hasFocus -> hint.label = label
            hint.label == label -> hint.label = null
        }
    }

private fun nowCaseCoverSize(sizes: NowPosterSizes, aspect: Float): DpSize =
    if (aspect >= sizes.caseWidth / sizes.caseHeight) {
        DpSize(sizes.caseWidth, sizes.caseWidth / aspect)
    } else {
        DpSize(sizes.caseHeight * aspect, sizes.caseHeight)
    }

@Composable
private fun NowPhysicalMedia(path: String, sizes: NowPosterSizes, modifier: Modifier) {
    val paths = remember(path) { listOf(path) }
    Box(modifier.size(sizes.caseWidth * MEDIA_SIZE_FRACTION)) {
        LibraryHeldArt(paths, ContentScale.Fit)
    }
}

@Composable
private fun NowCover(title: String, art: NowGameArt, modifier: Modifier) {
    val shape = JarnTheme.radius.smShape
    val cover = art.coverPath
    Box(
        modifier
            .clearAndSetSemantics {}
            .clip(shape),
    ) {
        when {
            !art.isLoaded -> Unit
            cover == null -> JarnArtPlaceholder(title, Modifier.matchParentSize())
            else -> {
                val paths = remember(cover) { listOf(cover) }
                LibraryHeldArt(paths, ContentScale.Fit) {
                    JarnArtPlaceholder(title, Modifier.matchParentSize())
                }
            }
        }
    }
}

@Composable
private fun NowPlayRow(
    game: JarnLibraryGame,
    controller: JarnGameActionsController,
    playFocus: FocusRequester,
    confirmHint: NowConfirmHint,
) {
    val setup = JarnGameActions.setup
    val shortcutUp = LocalNowShortcutUp.current
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        JarnButton(
            label = stringResource(R.string.jarnfe_hint_play),
            onClick = { JarnLibrary.play(game) },
            icon = painterResource(R.drawable.ic_jarn_play),
            modifier = Modifier
                .weight(1f)
                .nowConfirmHint(confirmHint, R.string.jarnfe_hint_play)
                .focusRequester(playFocus)
                .focusProperties { up = shortcutUp },
        )
        NowFavoriteButton(
            isFavorite = setup?.isFavorite == true,
            enabled = setup != null,
            onToggle = { controller.toggleFavorite(game) },
            modifier = Modifier.nowConfirmHint(confirmHint, R.string.jarnfe_game_favorite),
        )
    }
}

@Composable
private fun NowResumeRow(
    game: JarnLibraryGame,
    status: JarnGameFileStatus?,
    resumeFocus: FocusRequester,
    controller: JarnGameActionsController?,
    confirmHint: NowConfirmHint,
) {
    val setup = JarnGameActions.setup
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        NowResumeButton(
            game = game,
            status = status,
            focus = resumeFocus,
            modifier = Modifier.weight(1f).nowConfirmHint(confirmHint, R.string.jarnfe_hint_resume),
        )
        controller?.let {
            NowFavoriteButton(
                isFavorite = setup?.isFavorite == true,
                enabled = setup != null,
                onToggle = { it.toggleFavorite(game) },
                modifier = Modifier.nowConfirmHint(confirmHint, R.string.jarnfe_game_favorite),
            )
        }
    }
}

@Composable
private fun NowFavoriteButton(
    isFavorite: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.mdShape
    val label = stringResource(R.string.jarnfe_game_favorite)
    val state = stringResource(if (isFavorite) CoreR.string.jarn_state_on else CoreR.string.jarn_state_off)
    val icon = if (isFavorite) R.drawable.ic_jarn_heart_filled else R.drawable.ic_jarn_heart_outline
    Box(
        modifier = modifier
            .size(FavoriteSize)
            .jarnInteractive(shape, source, enabled, focusableWhenDisabled = true, role = Role.Switch, onClick = onToggle)
            .semantics {
                contentDescription = label
                stateDescription = state
            }
            .background(JarnTheme.colors.raised, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(FavoriteIconSize),
            colorFilter = ColorFilter.tint(JarnTheme.colors.textHigh),
        )
    }
}

@Composable
private fun NowMoreSheet(
    game: JarnLibraryGame,
    setupRows: (@Composable () -> Unit)?,
    confirmHint: NowConfirmHint,
) {
    val description = game.metadata.description?.trim()?.takeIf { it.isNotEmpty() }
    val entries = nowMetadataEntries(game.metadata)
    if (setupRows == null && description == null && entries.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarnTheme.colors.bg.copy(alpha = SHEET_ALPHA))
            .padding(
                start = JarnTheme.space.deckMargin,
                end = JarnTheme.space.deckMargin,
                top = JarnTheme.space.md,
                bottom = JarnTheme.space.focusEdge,
            ),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        setupRows?.let { rows ->
            Column(
                modifier = Modifier.nowConfirmHint(confirmHint, R.string.jarnfe_hint_open),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) { rows() }
        }
        description?.let { NowDescription(it) }
        if (entries.isNotEmpty()) NowReadable { JarnMetadataPlate(entries) }
    }
}

@Composable
private fun NowDescription(text: String) {
    NowReadable {
        JarnText(
            text = text,
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
            maxLines = DESCRIPTION_MAX_LINES,
            modifier = Modifier
                .fillMaxWidth()
                .background(JarnTheme.colors.surface, JarnTheme.radius.smShape)
                .padding(JarnTheme.space.md),
        )
    }
}

/** Lets the D-pad land on text, so the page can scroll all the way to it. */
@Composable
private fun NowReadable(content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .jarnFocus(JarnTheme.radius.smShape, source)
            .focusable(interactionSource = source),
    ) { content() }
}
