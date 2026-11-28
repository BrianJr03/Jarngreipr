package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.esde.EsdeFallbackNote
import jr.brian.frontend.look.esde.EsdeImportOutcome
import jr.brian.frontend.look.esde.EsdeTranslationNote
import jr.brian.frontend.look.esde.EsdeTranslationReport
import jr.brian.jarnfe.R

/**
 * The report in counts: parts that came over out of all the theme's parts, and how many need a
 * fix (left out). Notes about the whole theme aren't parts, so they only show as lines.
 */
internal data class EsdeReportSummary(
    val cameOver: Int,
    val total: Int,
    val changed: List<Pair<EsdeTranslationNote, Int>>,
    val leftOut: List<Pair<EsdeTranslationNote, Int>>,
) {
    val needFix: Int get() = total - cameOver
}

internal fun EsdeTranslationReport.summary(): EsdeReportSummary {
    val cameOver = translated.map { it.view to it.element }.toSet()
    val missing = notUsed.filter { it.element != null }.map { it.view to it.element }.toSet() - cameOver
    return EsdeReportSummary(
        cameOver = cameOver.size,
        total = cameOver.size + missing.size,
        changed = adjusted.mapNotNull { it.note }.counted(),
        leftOut = notUsed.mapNotNull { it.note }.counted(),
    )
}

private fun List<EsdeTranslationNote>.counted() = groupingBy { it }.eachCount().toList()

/** What the colors, art and fonts fallback did, folded so a long list of systems is one line. */
internal sealed interface EsdeFallbackLine {
    data class Text(@param:StringRes val res: Int, val arg: String? = null) : EsdeFallbackLine
    data class Count(val res: Int, val count: Int, val of: Int? = null) : EsdeFallbackLine
}

internal fun EsdeImportOutcome.Imported.fallbackLines(): List<EsdeFallbackLine> = buildList {
    fallbackNotes.filterIsInstance<EsdeFallbackNote.Colors>().firstOrNull()?.let { colors ->
        add(EsdeFallbackLine.Text(colors.tier.messageRes))
        colors.darkVersion?.let { add(EsdeFallbackLine.Text(R.string.jarnfe_esde_fallback_dark_version, it)) }
    }
    if (EsdeFallbackNote.LightThemeAccentOnly in fallbackNotes) add(EsdeFallbackLine.Text(R.string.jarnfe_esde_fallback_light_accent))
    if (systemCount > 0) add(EsdeFallbackLine.Count(R.plurals.jarnfe_esde_fallback_systems_with_art, systemsWithArt, systemCount))
    countOf<EsdeFallbackNote.UnknownSystem>(R.plurals.jarnfe_esde_fallback_unknown_systems)?.let(::add)
    val notCopied = fallbackNotes.count {
        it is EsdeFallbackNote.LogoNotCopied || it is EsdeFallbackNote.BackgroundNotCopied || it is EsdeFallbackNote.SvgNotCopied
    }
    if (notCopied > 0) add(EsdeFallbackLine.Count(R.plurals.jarnfe_esde_fallback_art_not_copied, notCopied))
    countOf<EsdeFallbackNote.FontRefused>(R.plurals.jarnfe_esde_fallback_fonts_refused)?.let(::add)
    if (EsdeFallbackNote.AssetsCapped in fallbackNotes) add(EsdeFallbackLine.Text(R.string.jarnfe_esde_fallback_capped))
    if (unusableFiles > 0) add(EsdeFallbackLine.Count(R.plurals.jarnfe_esde_import_unusable_files, unusableFiles))
}

private inline fun <reified T : EsdeFallbackNote> EsdeImportOutcome.Imported.countOf(res: Int): EsdeFallbackLine? =
    fallbackNotes.count { it is T }.takeIf { it > 0 }?.let { EsdeFallbackLine.Count(res, it) }

@get:StringRes
private val JarnPaletteMatch.messageRes: Int
    get() = when (this) {
        JarnPaletteMatch.FullPalette -> R.string.jarnfe_esde_fallback_full_palette
        JarnPaletteMatch.AccentOnSavedLook -> R.string.jarnfe_esde_fallback_accent_only
        JarnPaletteMatch.SavedLook -> R.string.jarnfe_esde_fallback_saved_look
    }

@get:StringRes
internal val EsdeTranslationNote.messageRes: Int
    get() = when (this) {
        EsdeTranslationNote.WheelAsShelf -> R.string.jarnfe_esde_note_wheel_as_shelf
        EsdeTranslationNote.MovedBelowHeaderBand -> R.string.jarnfe_esde_note_moved_below_header
        EsdeTranslationNote.SizeGuessed -> R.string.jarnfe_esde_note_size_guessed
        EsdeTranslationNote.TileAsCrop -> R.string.jarnfe_esde_note_tile_as_crop
        EsdeTranslationNote.PerSystemFileAsSystemArt -> R.string.jarnfe_esde_note_per_system_art
        EsdeTranslationNote.ShownOnSomeSystems -> R.string.jarnfe_esde_note_some_systems
        EsdeTranslationNote.MediaKindSkipped -> R.string.jarnfe_esde_note_media_skipped
        EsdeTranslationNote.StackedDetails -> R.string.jarnfe_esde_note_stacked_details
        EsdeTranslationNote.NameWhenNoLogo -> R.string.jarnfe_esde_note_name_when_no_logo
        EsdeTranslationNote.NarrowScreenLayout -> R.string.jarnfe_esde_note_narrow_layout
        EsdeTranslationNote.NarrowListTypeKept -> R.string.jarnfe_esde_note_narrow_list_kept
        EsdeTranslationNote.FontNotUsed -> R.string.jarnfe_esde_note_font_not_used
        EsdeTranslationNote.UnsupportedElement -> R.string.jarnfe_esde_note_unsupported_element
        EsdeTranslationNote.ExtraPrimary -> R.string.jarnfe_esde_note_extra_primary
        EsdeTranslationNote.UnsupportedMedia -> R.string.jarnfe_esde_note_unsupported_media
        EsdeTranslationNote.DiffersPerSystem -> R.string.jarnfe_esde_note_differs_per_system
        EsdeTranslationNote.UnsupportedField -> R.string.jarnfe_esde_note_unsupported_field
        EsdeTranslationNote.NothingReadable -> R.string.jarnfe_esde_note_nothing_readable
        EsdeTranslationNote.Hidden -> R.string.jarnfe_esde_note_hidden
        EsdeTranslationNote.NoPrimary -> R.string.jarnfe_esde_note_no_primary
        EsdeTranslationNote.LegacyTheme -> R.string.jarnfe_esde_note_legacy
        EsdeTranslationNote.TransitionsAndTriggers -> R.string.jarnfe_esde_note_transitions
    }

/** "31 of 34 came over. 3 need a fix.", then what changed, what was left out, and the fallback lines. */
@Composable
internal fun EsdeImportReport(outcome: EsdeImportOutcome.Imported, modifier: Modifier = Modifier) {
    val summary = outcome.report.summary()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        ReportHeadline(summary)
        NoteList(R.string.jarnfe_esde_import_changed_title, summary.changed)
        NoteList(R.string.jarnfe_esde_import_left_out_title, summary.leftOut)
        outcome.fallbackLines().forEach { ReportLine(it.text()) }
    }
}

@Composable
private fun ReportHeadline(summary: EsdeReportSummary) {
    val cameOver = pluralStringResource(R.plurals.jarnfe_esde_import_came_over, summary.total, summary.cameOver, summary.total)
    val text = if (summary.needFix == 0) {
        stringResource(R.string.jarnfe_esde_import_everything)
    } else {
        cameOver + " " + pluralStringResource(R.plurals.jarnfe_esde_import_need_fix, summary.needFix, summary.needFix)
    }
    JarnText(text, JarnTheme.type.heading, JarnTheme.colors.textHigh)
}

@Composable
private fun NoteList(@StringRes titleRes: Int, notes: List<Pair<EsdeTranslationNote, Int>>) {
    if (notes.isEmpty()) return
    JarnText(stringResource(titleRes), JarnTheme.type.labelStrong, JarnTheme.colors.textMid)
    notes.forEach { (note, count) ->
        val line = stringResource(note.messageRes)
        ReportLine(if (count > 1) stringResource(R.string.jarnfe_esde_import_note_count, line, count) else line)
    }
}

@Composable
private fun ReportLine(text: String) {
    JarnText(text, JarnTheme.type.body, JarnTheme.colors.textMid)
}

@Composable
private fun EsdeFallbackLine.text(): String = when (this) {
    is EsdeFallbackLine.Text -> arg?.let { stringResource(res, it) } ?: stringResource(res)
    is EsdeFallbackLine.Count -> of?.let { pluralStringResource(res, count, count, it) } ?: pluralStringResource(res, count, count)
}
