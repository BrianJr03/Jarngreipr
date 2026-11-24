package jr.brian.jarnfe.firstrun

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.model.ArtSource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnKeyCap
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.art.ArtSourceStatus
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnFolderPicker
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryStatus
import jr.brian.jarnfe.library.jarnArtDetectorOrNull
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.library.jarnLibrarySetupOrNull
import jr.brian.jarnfe.library.librarySetupPrompt
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.settings.JarnLookPicker
import jr.brian.jarnfe.settings.labelRes
import jr.brian.core.ui.theme.JarnLooks

@Composable
internal fun FirstRunStepContent(
    step: JarnFirstRunStep,
    run: JarnFirstRunState,
    twoScreens: Boolean,
    actions: JarnFirstRunActions,
) {
    when (step) {
        JarnFirstRunStep.Welcome -> FirstRunHeading(
            R.string.jarnfe_first_run_welcome_title,
            if (twoScreens) R.string.jarnfe_first_run_welcome_body else R.string.jarnfe_first_run_welcome_body_single,
        )
        JarnFirstRunStep.Library -> FirstRunLibraryStep()
        JarnFirstRunStep.Media -> FirstRunMediaStep(actions)
        JarnFirstRunStep.Look -> FirstRunLookStep()
        JarnFirstRunStep.SwitchCue -> FirstRunSwitchCueStep(actions)
        JarnFirstRunStep.Done -> FirstRunDoneStep(twoScreens)
        else -> FirstRunLesson(step, run, twoScreens)
    }
}

@Composable
private fun FirstRunLibraryStep() {
    val context = LocalContext.current
    val setup = remember(context) { context.jarnLibrarySetupOrNull() }
    val setupState = setup?.state?.collectAsState()?.value
    val systems = JarnLibrary.state.systems
    val prompt = librarySetupPrompt(setupState, systems)
    when {
        systems.items.isNotEmpty() -> FirstRunExistingLibrary(systems.items.size, systems.items.sumOf { it.gameCount })
        prompt != null -> {
            JarnText(
                text = stringResource(prompt.titleRes),
                style = JarnTheme.type.heading,
                color = JarnTheme.colors.textHigh,
                maxLines = 2,
            )
            JarnText(
                text = prompt.folderName?.let { stringResource(prompt.messageRes, it) }
                    ?: stringResource(prompt.messageRes),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textMid,
            )
            FirstRunNote(R.string.jarnfe_first_run_library_why)
            JarnButton(
                label = stringResource(prompt.actionRes),
                onClick = { JarnFolderPicker.open?.invoke(null) },
                variant = JarnButtonVariant.Secondary,
            )
        }
        systems.status is JarnLibraryStatus.Loading -> FirstRunHeading(
            R.string.jarnfe_first_run_library_title,
            R.string.jarnfe_first_run_library_loading,
        )
        else -> FirstRunHeading(
            R.string.jarnfe_first_run_library_title,
            R.string.jarnfe_first_run_library_later,
        )
    }
}

@Composable
private fun FirstRunExistingLibrary(systemCount: Int, gameCount: Int) {
    JarnText(
        text = stringResource(R.string.jarnfe_first_run_existing_title),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        maxLines = 2,
    )
    JarnText(
        text = stringResource(
            R.string.jarnfe_first_run_existing_counts,
            pluralStringResource(R.plurals.jarnfe_first_run_existing_systems, systemCount, systemCount),
            pluralStringResource(R.plurals.jarnfe_first_run_existing_games, gameCount, gameCount),
        ),
        style = JarnTheme.type.labelStrong,
        color = JarnTheme.colors.textHigh,
    )
    JarnText(
        text = stringResource(R.string.jarnfe_first_run_existing_body),
        style = JarnTheme.type.body,
        color = JarnTheme.colors.textMid,
    )
}

@Composable
private fun FirstRunMediaStep(actions: JarnFirstRunActions) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnLibraryDataOrNull() }
    if (data == null) {
        FirstRunHeading(R.string.jarnfe_first_run_media_title, R.string.jarnfe_first_run_media_none)
        return
    }
    val summary = rememberFirstRunMedia(data)
    FirstRunHeading(R.string.jarnfe_first_run_media_title, R.string.jarnfe_first_run_media_intro)
    FirstRunMediaResult(summary)
    if (summary == JarnMediaSummary.Missing && !Environment.isExternalStorageManager()) {
        FirstRunFileAccessButton()
    }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_first_run_media_open),
        onClick = actions::openArtSettings,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_first_run_media_open_hint),
    )
}

/** Rechecks on every return, because file access is granted in a system screen. */
@Composable
private fun rememberFirstRunMedia(data: JarnLibraryData): JarnMediaSummary {
    val context = LocalContext.current
    val detector = remember(context) { context.jarnArtDetectorOrNull() }
    val owner = LocalLifecycleOwner.current
    var resumes by remember { mutableIntStateOf(0) }
    DisposableEffect(owner, detector) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                detector?.check()
                resumes++
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val detected = detector?.detected?.collectAsState()?.value
    val selected by data.artSource.collectAsState()
    val revision by data.artRevision.collectAsState()
    val statuses by produceState<List<ArtSourceStatus>?>(null, revision, detected, resumes) {
        value = data.artSourceStatus()
    }
    return jarnMediaSummary(detected, selected, statuses)
}

@Composable
private fun FirstRunMediaResult(summary: JarnMediaSummary) {
    JarnSurface(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(JarnTheme.space.md),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            JarnText(
                text = mediaHeadline(summary),
                style = JarnTheme.type.labelStrong,
                color = JarnTheme.colors.textHigh,
            )
            mediaDetailRes(summary)?.let { detail ->
                JarnText(stringResource(detail), JarnTheme.type.caption, JarnTheme.colors.textMid)
            }
        }
    }
}

@Composable
private fun mediaHeadline(summary: JarnMediaSummary): String = when (summary) {
    JarnMediaSummary.Checking -> stringResource(R.string.jarnfe_first_run_media_checking)
    is JarnMediaSummary.Detected -> stringResource(R.string.jarnfe_first_run_media_found, sourceName(summary.source))
    is JarnMediaSummary.InUse -> stringResource(R.string.jarnfe_first_run_media_in_use, sourceName(summary.source))
    is JarnMediaSummary.Elsewhere -> stringResource(R.string.jarnfe_first_run_media_elsewhere, sourceName(summary.source))
    JarnMediaSummary.Missing -> stringResource(R.string.jarnfe_first_run_media_none)
}

@StringRes
private fun mediaDetailRes(summary: JarnMediaSummary): Int? = when {
    summary is JarnMediaSummary.Detected && summary.wonOverOther -> R.string.jarnfe_first_run_media_esde_wins
    summary is JarnMediaSummary.Elsewhere -> R.string.jarnfe_first_run_media_elsewhere_hint
    summary == JarnMediaSummary.Missing -> R.string.jarnfe_first_run_media_none_hint
    else -> null
}

@Composable
private fun sourceName(source: ArtSource): String = stringResource(source.labelRes())

@Composable
private fun FirstRunFileAccessButton() {
    val activity = LocalActivity.current
    FirstRunNote(R.string.jarnfe_first_run_media_access_why)
    JarnButton(
        label = stringResource(R.string.jarnfe_first_run_media_access),
        onClick = {
            val target = activity ?: return@JarnButton
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${target.packageName}".toUri(),
            )
            runCatching { target.startActivity(intent) }
        },
        variant = JarnButtonVariant.Secondary,
    )
}

@Composable
private fun FirstRunLookStep() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    FirstRunHeading(R.string.jarnfe_first_run_look_title, R.string.jarnfe_first_run_look_body)
    if (store == null) return
    val savedId by store.lookIdState.collectAsState()
    JarnLookPicker(chosen = JarnLooks.fromId(savedId), onChoose = { store.setLookId(it.id) })
}

@Composable
private fun FirstRunLesson(step: JarnFirstRunStep, run: JarnFirstRunState, twoScreens: Boolean) {
    val lesson = lessonFor(step, twoScreens)
    FirstRunHeading(lesson.titleRes, lesson.bodyRes)
    FirstRunKeys(lesson.keys)
    lesson.noteRes?.let { FirstRunNote(it) }
    if (step == JarnSession.state.practiceTabsStepOn(JarnScreen.Deck)) FirstRunPracticeTabs(run.practiceTab)
    if (run.practiced) {
        JarnText(
            text = stringResource(R.string.jarnfe_first_run_nice),
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.success,
            maxLines = 1,
        )
    }
}

/** [noteRes] is one extra line under the keys, such as holding X to swap the screens. */
private class FirstRunLessonCopy(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val keys: List<JarnKey>,
    @param:StringRes val noteRes: Int? = null,
)

private fun lessonFor(step: JarnFirstRunStep, twoScreens: Boolean): FirstRunLessonCopy = when (step) {
    JarnFirstRunStep.PressB -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_b_title,
        R.string.jarnfe_first_run_b_body,
        listOf(JarnKey.B),
    )
    JarnFirstRunStep.PressX -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_options_title,
        if (twoScreens) R.string.jarnfe_first_run_options_body else R.string.jarnfe_first_run_options_body_single,
        listOf(JarnKey.Select),
    )
    JarnFirstRunStep.PressSelect -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_switch_title,
        R.string.jarnfe_first_run_switch_body,
        listOf(JarnKey.X),
        noteRes = R.string.jarnfe_first_run_switch_hold,
    )
    JarnFirstRunStep.PressTabs -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_tabs_title,
        if (twoScreens) R.string.jarnfe_first_run_tabs_body else R.string.jarnfe_first_run_tabs_body_single,
        listOf(JarnKey.L1, JarnKey.R1),
    )
    JarnFirstRunStep.PressOtherTabs -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_other_tabs_title,
        R.string.jarnfe_first_run_other_tabs_body,
        listOf(JarnKey.L2, JarnKey.R2),
    )
    else -> FirstRunLessonCopy(
        R.string.jarnfe_first_run_a_title,
        R.string.jarnfe_first_run_a_body,
        listOf(JarnKey.A),
    )
}

@Composable
private fun FirstRunDoneStep(twoScreens: Boolean) {
    FirstRunHeading(R.string.jarnfe_first_run_done_title, R.string.jarnfe_first_run_done_body)
    FirstRunCheatRow(listOf(JarnKey.A), R.string.jarnfe_first_run_cheat_a)
    FirstRunCheatRow(listOf(JarnKey.B), R.string.jarnfe_first_run_cheat_b)
    FirstRunCheatRow(listOf(JarnKey.Select), R.string.jarnfe_hint_context)
    FirstRunCheatRow(listOf(JarnKey.Y), R.string.jarnfe_hint_search)
    FirstRunCheatRow(listOf(JarnKey.Start), R.string.jarnfe_route_settings)
    FirstRunCheatRow(
        listOf(JarnKey.X),
        if (twoScreens) R.string.jarnfe_first_run_cheat_switch else R.string.jarnfe_first_run_cheat_switch_single,
    )
    FirstRunCheatRow(
        listOf(JarnKey.L1, JarnKey.R1),
        if (twoScreens) R.string.jarnfe_first_run_cheat_tabs else R.string.jarnfe_first_run_cheat_tabs_single,
    )
    if (twoScreens) FirstRunCheatRow(listOf(JarnKey.L2, JarnKey.R2), R.string.jarnfe_first_run_cheat_other_tabs)
}

/** Focusable so the D-pad can scroll the whole list on a short screen. */
@Composable
private fun FirstRunCheatRow(keys: List<JarnKey>, @StringRes labelRes: Int) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .jarnFocus(JarnTheme.radius.smShape, source)
            .focusable(interactionSource = source)
            .padding(JarnTheme.space.xs),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { JarnKeyCap(it) }
        JarnText(stringResource(labelRes), JarnTheme.type.body, JarnTheme.colors.textHigh)
    }
}

@Composable
private fun FirstRunNote(@StringRes textRes: Int) {
    JarnText(stringResource(textRes), JarnTheme.type.caption, JarnTheme.colors.textMid)
}
