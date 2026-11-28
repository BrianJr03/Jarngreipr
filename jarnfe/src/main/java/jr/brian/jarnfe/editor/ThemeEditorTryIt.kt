package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.applyThemeChoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Try it: the editor steps aside and the player browses their library with the draft, for real,
 * on both screens. Games don't start, and Start offers Back to editing or Use this theme.
 */
internal object ThemeEditorTryIt {
    var active by mutableStateOf(false)
        private set

    /** The line shown above the hint bar: what Try it is on entry, then why a game didn't start. */
    var notice by mutableStateOf<TryItNotice?>(null)
        private set

    private var editorStack: List<StageRoute> = emptyList()
    private var noticeCount = 0L

    fun start() {
        val session = JarnSession.state
        if (session.stageRoute != StageRoute.ThemeEditor) return
        editorStack = session.stageStack
        active = true
        say(R.string.jarnfe_editor_try_strip)
        JarnLibrary.playGuard = { say(R.string.jarnfe_editor_try_no_games) }
        JarnSession.update { openRoute(StageRoute.Library) }
    }

    fun dismiss(shown: TryItNotice) {
        if (notice == shown) notice = null
    }

    private fun say(@StringRes messageRes: Int) {
        noticeCount += 1
        notice = TryItNotice(messageRes, noticeCount)
    }

    /** Back to the editor, with the routes it was opened over. */
    fun backToEditing() {
        end()
        val stack = editorStack
        JarnSession.update { copy(stageStack = stack).openThemeEditor() }
    }

    /** Saves the draft, makes it the active theme and closes the editor. */
    suspend fun use(lookStore: JarnLookStore?) {
        JarnThemeEditor.saveVersion()
        JarnThemeEditor.state?.let { lookStore?.applyThemeChoice(JarnThemeChoice(it.entry)) }
        end()
        JarnThemeEditor.close()
    }

    private fun end() {
        active = false
        notice = null
        JarnLibrary.playGuard = null
    }
}

/**
 * On Stage while trying: a short notice above the hint bar, Start for the choices, and the draft
 * on every screen. Nothing covers the tabs, and notices clear after a few seconds.
 * Sits above the shell's Start, so Settings can't open mid-try.
 */
@Composable
internal fun BoxScope.ThemeEditorTryItHost() {
    if (!ThemeEditorTryIt.active) return
    val editor = JarnThemeEditor.state ?: return
    val context = LocalContext.current
    val lookStore = remember(context) { context.jarnLookStoreOrNull() }
    val scope = rememberCoroutineScope()
    var choicesOpen by remember { mutableStateOf(false) }
    DraftPreviewEffect(editor)
    JarnInputLayer(
        enabled = !choicesOpen,
        bindings = { on(JarnAction.Menu, R.string.jarnfe_editor_try_options) { choicesOpen = true } },
    )
    TryItNoticeLine(Modifier.align(Alignment.BottomCenter))
    JarnSheet(
        visible = choicesOpen,
        onDismiss = { choicesOpen = false },
        title = stringResource(R.string.jarnfe_editor_try_title, editor.spec.meta.name),
    ) {
        InspectActionRow(stringResource(R.string.jarnfe_editor_try_back)) {
            choicesOpen = false
            ThemeEditorTryIt.backToEditing()
        }
        InspectActionRow(stringResource(R.string.jarnfe_editor_try_use), stringResource(R.string.jarnfe_editor_try_use_hint)) {
            choicesOpen = false
            scope.launch { ThemeEditorTryIt.use(lookStore) }
        }
    }
}


@Composable
private fun TryItNoticeLine(modifier: Modifier) {
    val notice = ThemeEditorTryIt.notice ?: return
    LaunchedEffect(notice) {
        delay(TRY_IT_NOTICE_MS)
        ThemeEditorTryIt.dismiss(notice)
    }
    JarnSurface(modifier = modifier.padding(bottom = JarnTheme.space.hintBarHeight + JarnTheme.space.lg)) {
        Box(Modifier.padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.md)) {
            JarnText(stringResource(notice.messageRes), JarnTheme.type.body, JarnTheme.colors.textHigh, maxLines = 2)
        }
    }
}

/** One Try it message; [sequence] makes a repeat of the same message show again. */
internal data class TryItNotice(@param:StringRes val messageRes: Int, val sequence: Long)

/** As long as Jarn's other notices stay up. */
private const val TRY_IT_NOTICE_MS = 5_000L
