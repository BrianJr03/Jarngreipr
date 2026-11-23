package jr.brian.jarnfe.scraper

import androidx.compose.runtime.Immutable
import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperMediaKind

internal enum class ScraperStep { Source, Account, Test, Art, Finish }

internal enum class ScraperField(val source: JarnScraperSource, val isSecret: Boolean) {
    UserId(JarnScraperSource.ScreenScraper, isSecret = false),
    Password(JarnScraperSource.ScreenScraper, isSecret = true),
    ApiKey(JarnScraperSource.SteamGridDb, isSecret = true),
}

internal val ScreenScraperArtKinds = listOf(
    ScraperMediaKind.Covers,
    ScraperMediaKind.Screenshots,
    ScraperMediaKind.Marquees,
    ScraperMediaKind.Fanart,
    ScraperMediaKind.TitleScreens,
)

internal val SteamGridDbArtKinds = listOf(
    ScraperMediaKind.Covers,
    ScraperMediaKind.Marquees,
    ScraperMediaKind.Fanart,
)

internal fun JarnScraperSource.artKinds(): List<ScraperMediaKind> = when (this) {
    JarnScraperSource.ScreenScraper -> ScreenScraperArtKinds
    JarnScraperSource.SteamGridDb -> SteamGridDbArtKinds
}

internal data class ScraperSelection(
    val useScreenScraper: Boolean,
    val useSteamGridDb: Boolean,
    val screenScraperKinds: Set<ScraperMediaKind>,
    val steamGridDbKinds: Set<ScraperMediaKind>,
)

/** Lives only in memory: it is never saved to instance state and its text form is redacted. */
@Immutable
internal data class ScraperWizard(
    val initial: JarnScraperStatus,
    val step: ScraperStep = ScraperStep.Source,
    val useScreenScraper: Boolean,
    val useSteamGridDb: Boolean,
    val userId: String? = null,
    val password: String? = null,
    val apiKey: String? = null,
    val screenScraperKinds: Set<ScraperMediaKind>,
    val steamGridDbKinds: Set<ScraperMediaKind>,
    val results: Map<JarnScraperSource, ScraperConnectionResult> = emptyMap(),
    val isTesting: Boolean = false,
    val start: ScraperSelection = ScraperSelection(
        useScreenScraper,
        useSteamGridDb,
        screenScraperKinds,
        steamGridDbKinds,
    ),
) {
    val sources: List<JarnScraperSource>
        get() = buildList {
            if (useScreenScraper) add(JarnScraperSource.ScreenScraper)
            if (useSteamGridDb) add(JarnScraperSource.SteamGridDb)
        }

    val fields: List<ScraperField>
        get() = ScraperField.entries.filter { it.source in sources }

    val draft: JarnScraperDraft
        get() = JarnScraperDraft(userId, password, apiKey)

    private val selection: ScraperSelection
        get() = ScraperSelection(useScreenScraper, useSteamGridDb, screenScraperKinds, steamGridDbKinds)

    /** Compared with where the wizard opened, so its suggested defaults are not a user edit. */
    val isDirty: Boolean
        get() = userId != null || password != null || apiKey != null || selection != start

    val allPassed: Boolean
        get() = sources.isNotEmpty() && sources.all { results[it] == ScraperConnectionResult.Ok }

    val canContinue: Boolean
        get() = when (step) {
            ScraperStep.Source -> sources.isNotEmpty()
            ScraperStep.Account -> fields.all(::hasValue)
            ScraperStep.Test -> allPassed && !isTesting
            ScraperStep.Art -> sources.all { kindsFor(it).isNotEmpty() }
            ScraperStep.Finish -> true
        }

    fun hasValue(field: ScraperField): Boolean = when (field) {
        ScraperField.UserId -> userId != null || initial.screenScraperSaved
        ScraperField.Password -> password != null || initial.screenScraperSaved
        ScraperField.ApiKey -> apiKey != null || initial.steamGridDbSaved
    }

    fun isEdited(field: ScraperField): Boolean = when (field) {
        ScraperField.UserId -> userId != null
        ScraperField.Password -> password != null
        ScraperField.ApiKey -> apiKey != null
    }

    fun kindsFor(source: JarnScraperSource): Set<ScraperMediaKind> = when (source) {
        JarnScraperSource.ScreenScraper -> screenScraperKinds
        JarnScraperSource.SteamGridDb -> steamGridDbKinds
    }

    fun toggleSource(source: JarnScraperSource): ScraperWizard = when (source) {
        JarnScraperSource.ScreenScraper ->
            if (initial.screenScraperAvailable) copy(useScreenScraper = !useScreenScraper) else this
        JarnScraperSource.SteamGridDb -> copy(useSteamGridDb = !useSteamGridDb)
    }

    /** A blank entry keeps the saved value. Any change means the old test no longer counts. */
    fun edit(field: ScraperField, value: String): ScraperWizard {
        val entry = value.takeIf { it.isNotBlank() }
        val edited = when (field) {
            ScraperField.UserId -> copy(userId = entry)
            ScraperField.Password -> copy(password = entry)
            ScraperField.ApiKey -> copy(apiKey = entry)
        }
        return edited.copy(results = results - field.source)
    }

    fun toggleKind(source: JarnScraperSource, kind: ScraperMediaKind): ScraperWizard = when (source) {
        JarnScraperSource.ScreenScraper -> copy(screenScraperKinds = screenScraperKinds.toggled(kind))
        JarnScraperSource.SteamGridDb -> copy(steamGridDbKinds = steamGridDbKinds.toggled(kind))
    }

    fun next(): ScraperWizard = if (canContinue) advance() else this

    fun skipTest(): ScraperWizard =
        if (step == ScraperStep.Test && !isTesting) advance() else this

    fun previous(): ScraperWizard =
        copy(step = ScraperStep.entries[(step.ordinal - 1).coerceAtLeast(0)], isTesting = false)

    fun toPlan() = JarnScraperPlan(
        useScreenScraper = useScreenScraper,
        useSteamGridDb = useSteamGridDb,
        screenScraperKinds = screenScraperKinds,
        steamGridDbKinds = steamGridDbKinds,
        draft = draft,
    )

    override fun toString(): String = "ScraperWizard(step=$step)"

    private fun advance() = copy(
        step = ScraperStep.entries[(step.ordinal + 1).coerceAtMost(ScraperStep.entries.lastIndex)],
    )

    private fun Set<ScraperMediaKind>.toggled(kind: ScraperMediaKind) =
        if (kind in this) this - kind else this + kind

    companion object {
        fun from(status: JarnScraperStatus): ScraperWizard {
            val anyEnabled = status.screenScraperEnabled || status.steamGridDbEnabled
            val useScreenScraper = status.screenScraperAvailable &&
                (status.screenScraperEnabled || !anyEnabled)
            val useSteamGridDb = status.steamGridDbEnabled ||
                (!anyEnabled && !status.screenScraperAvailable)
            return ScraperWizard(
                initial = status,
                useScreenScraper = useScreenScraper,
                useSteamGridDb = useSteamGridDb,
                screenScraperKinds = status.screenScraperKinds.supportedOr(ScreenScraperArtKinds),
                steamGridDbKinds = status.steamGridDbKinds.supportedOr(SteamGridDbArtKinds),
            )
        }

        private fun Set<ScraperMediaKind>.supportedOr(all: List<ScraperMediaKind>) =
            intersect(all.toSet()).ifEmpty { all.toSet() }
    }
}
