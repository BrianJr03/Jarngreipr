package jr.brian.jarnfe.scraper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnStepper
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.jarnfe.R

@Composable
internal fun ScraperFieldKeyboard(
    field: ScraperField,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    JarnControllerKeyboard(
        label = stringResource(field.labelRes()),
        masked = field.isSecret,
        onDone = onDone,
        onCancel = onCancel,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun ScraperWizardContent(
    wizard: ScraperWizard,
    setup: JarnScraperSetup,
    refocus: Boolean,
    onChange: (ScraperWizard) -> Unit,
    onEdit: (ScraperField) -> Unit,
    onFinish: (scrapeNow: Boolean) -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(wizard.step, refocus) { if (refocus) first.requestFocus() }
    JarnStepper(wizard.step.ordinal, ScraperStep.entries.size)
    JarnText(
        text = stringResource(wizard.step.titleRes()),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
    )
    JarnText(
        text = stringResource(wizard.step.messageRes()),
        style = JarnTheme.type.body,
        color = JarnTheme.colors.textMid,
    )
    val firstModifier = Modifier.fillMaxWidth().focusRequester(first)
    when (wizard.step) {
        ScraperStep.Source -> SourceStep(wizard, firstModifier, onChange)
        ScraperStep.Account -> AccountStep(wizard, firstModifier, onChange, onEdit)
        ScraperStep.Test -> TestStep(wizard, setup, firstModifier, onChange)
        ScraperStep.Art -> ArtStep(wizard, firstModifier, onChange)
        ScraperStep.Finish -> FinishStep(firstModifier, onFinish)
    }
}

@Composable
private fun SourceStep(
    wizard: ScraperWizard,
    firstModifier: Modifier,
    onChange: (ScraperWizard) -> Unit,
) {
    val available = wizard.initial.screenScraperAvailable
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_scraper_source_screenscraper),
        checked = wizard.useScreenScraper,
        onCheckedChange = { onChange(wizard.toggleSource(JarnScraperSource.ScreenScraper)) },
        modifier = firstModifier,
        supporting = stringResource(
            if (available) R.string.jarnfe_scraper_screenscraper_hint else R.string.jarnfe_scraper_unavailable,
        ),
        enabled = available,
        focusableWhenDisabled = true,
    )
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_scraper_source_steamgriddb),
        checked = wizard.useSteamGridDb,
        onCheckedChange = { onChange(wizard.toggleSource(JarnScraperSource.SteamGridDb)) },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_scraper_steamgriddb_hint),
    )
    ContinueButton(wizard, onChange)
}

@Composable
private fun AccountStep(
    wizard: ScraperWizard,
    firstModifier: Modifier,
    onChange: (ScraperWizard) -> Unit,
    onEdit: (ScraperField) -> Unit,
) {
    wizard.fields.forEachIndexed { index, field ->
        JarnNavigateRow(
            label = stringResource(field.labelRes()),
            onClick = { onEdit(field) },
            modifier = if (index == 0) firstModifier else Modifier.fillMaxWidth(),
            supporting = stringResource(fieldStateRes(wizard, field)),
        )
    }
    JarnText(
        text = stringResource(R.string.jarnfe_scraper_account_help),
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
    )
    ContinueButton(wizard, onChange)
}

private fun fieldStateRes(wizard: ScraperWizard, field: ScraperField): Int = when {
    wizard.isEdited(field) -> R.string.jarnfe_scraper_field_entered
    wizard.hasValue(field) -> R.string.jarnfe_scraper_field_saved
    else -> R.string.jarnfe_scraper_field_empty
}

@Composable
private fun TestStep(
    wizard: ScraperWizard,
    setup: JarnScraperSetup,
    firstModifier: Modifier,
    onChange: (ScraperWizard) -> Unit,
) {
    if (wizard.isTesting) {
        LaunchedEffect(Unit) {
            val results = wizard.sources.associateWith { setup.test(it, wizard.draft) }
            onChange(wizard.copy(results = results, isTesting = false))
        }
    }
    wizard.sources.forEach { source ->
        ScraperResultLine(source, wizard.results[source], wizard.isTesting)
    }
    JarnButton(
        label = stringResource(R.string.jarnfe_scraper_test),
        onClick = { onChange(wizard.copy(isTesting = true, results = emptyMap())) },
        modifier = firstModifier,
        variant = if (wizard.allPassed) JarnButtonVariant.Secondary else JarnButtonVariant.Primary,
        loading = wizard.isTesting,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        JarnButton(
            label = stringResource(R.string.jarnfe_scraper_skip_test),
            onClick = { onChange(wizard.skipTest()) },
            modifier = Modifier.weight(1f),
            variant = JarnButtonVariant.Secondary,
            enabled = !wizard.isTesting,
            focusableWhenDisabled = true,
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_scraper_continue),
            onClick = { onChange(wizard.next()) },
            modifier = Modifier.weight(1f),
            enabled = wizard.canContinue,
            focusableWhenDisabled = true,
        )
    }
}

@Composable
internal fun ScraperResultLine(
    source: JarnScraperSource,
    result: ScraperConnectionResult?,
    isTesting: Boolean,
) {
    val colors = JarnTheme.colors
    val message = when {
        isTesting -> stringResource(R.string.jarnfe_scraper_testing)
        result == null -> stringResource(R.string.jarnfe_scraper_not_tested)
        else -> stringResource(result.messageRes(source))
    }
    JarnText(
        text = stringResource(R.string.jarnfe_scraper_result_line, stringResource(source.labelRes()), message),
        style = JarnTheme.type.label,
        color = when (result) {
            null -> colors.textMid
            ScraperConnectionResult.Ok -> colors.success
            else -> colors.danger
        },
    )
}

@Composable
private fun ArtStep(
    wizard: ScraperWizard,
    firstModifier: Modifier,
    onChange: (ScraperWizard) -> Unit,
) {
    wizard.sources.forEachIndexed { sourceIndex, source ->
        JarnText(
            text = stringResource(source.labelRes()),
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.textMid,
        )
        source.artKinds().forEachIndexed { kindIndex, kind ->
            JarnToggleRow(
                label = stringResource(kind.labelRes()),
                checked = kind in wizard.kindsFor(source),
                onCheckedChange = { onChange(wizard.toggleKind(source, kind)) },
                modifier = if (sourceIndex == 0 && kindIndex == 0) firstModifier else Modifier.fillMaxWidth(),
            )
        }
    }
    if (!wizard.canContinue) {
        JarnText(
            text = stringResource(R.string.jarnfe_scraper_art_required),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.danger,
        )
    }
    ContinueButton(wizard, onChange)
}

@Composable
private fun FinishStep(firstModifier: Modifier, onFinish: (Boolean) -> Unit) {
    JarnButton(
        label = stringResource(R.string.jarnfe_scraper_scrape_now),
        onClick = { onFinish(true) },
        modifier = firstModifier,
    )
    JarnButton(
        label = stringResource(R.string.jarnfe_scraper_later),
        onClick = { onFinish(false) },
        modifier = Modifier.fillMaxWidth(),
        variant = JarnButtonVariant.Secondary,
    )
}

@Composable
private fun ContinueButton(wizard: ScraperWizard, onChange: (ScraperWizard) -> Unit) {
    JarnButton(
        label = stringResource(R.string.jarnfe_scraper_continue),
        onClick = { onChange(wizard.next()) },
        modifier = Modifier.fillMaxWidth(),
        enabled = wizard.canContinue,
        focusableWhenDisabled = true,
    )
}
