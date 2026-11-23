package jr.brian.jarnfe.apps

import android.content.Context
import androidx.compose.runtime.Immutable
import jr.brian.jarnfe.dock.JarnDockApp
import kotlinx.coroutines.flow.Flow

internal const val JARN_APPS_HIDDEN_KEY = "jarnfe-apps-hidden-header"

@Immutable
data class JarnAppsSnapshot(
    val apps: List<JarnDockApp>,
    val pinned: List<String>,
    val hidden: Set<String>,
)

interface JarnAppsData : JarnAppOptionsData {
    val snapshot: Flow<JarnAppsSnapshot>
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnAppsDataProvider {
    val jarnAppsData: JarnAppsData
}

internal fun Context.jarnAppsDataOrNull(): JarnAppsData? =
    (applicationContext as? JarnAppsDataProvider)?.jarnAppsData

@Immutable
internal data class JarnAppSections(
    val pinned: List<JarnDockApp>,
    val all: List<JarnDockApp>,
    val hidden: List<JarnDockApp>,
) {
    val isEmpty: Boolean get() = pinned.isEmpty() && all.isEmpty() && hidden.isEmpty()
}

private val AppOrder = compareBy<JarnDockApp, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
    .thenBy { it.packageName }

/** Each app lands in one section. Hidden wins over Pinned, and the pin returns on unhide. */
internal fun jarnAppSections(snapshot: JarnAppsSnapshot): JarnAppSections {
    val byPackage = snapshot.apps.associateBy { it.packageName }
    val pinned = snapshot.pinned.distinct()
        .filter { it !in snapshot.hidden }
        .mapNotNull(byPackage::get)
    val pinnedPackages = pinned.mapTo(HashSet()) { it.packageName }
    val (hidden, visible) = snapshot.apps.partition { it.packageName in snapshot.hidden }
    return JarnAppSections(
        pinned = pinned,
        all = visible.filter { it.packageName !in pinnedPackages }.sortedWith(AppOrder),
        hidden = hidden.sortedWith(AppOrder),
    )
}

internal enum class JarnAppsSection { Pinned, All, Hidden }

internal sealed interface JarnAppsRow {
    val key: String

    @Immutable
    data class Apps(
        override val key: String,
        val section: JarnAppsSection,
        val showsTitle: Boolean,
        val apps: List<JarnDockApp>,
    ) : JarnAppsRow

    @Immutable
    data class HiddenHeader(val count: Int, val expanded: Boolean) : JarnAppsRow {
        override val key: String = JARN_APPS_HIDDEN_KEY
    }
}

private fun sectionRows(
    section: JarnAppsSection,
    apps: List<JarnDockApp>,
    columns: Int,
    titled: Boolean,
): List<JarnAppsRow> = apps.chunked(columns.coerceAtLeast(1)).mapIndexed { index, row ->
    JarnAppsRow.Apps("${section.name}:$index", section, titled && index == 0, row)
}

/** Titles live inside a section's first row, so focusing that row always reveals its title. */
internal fun jarnAppRows(sections: JarnAppSections, expanded: Boolean, columns: Int): List<JarnAppsRow> =
    buildList {
        addAll(sectionRows(JarnAppsSection.Pinned, sections.pinned, columns, titled = true))
        addAll(sectionRows(JarnAppsSection.All, sections.all, columns, titled = true))
        if (sections.hidden.isNotEmpty()) {
            add(JarnAppsRow.HiddenHeader(sections.hidden.size, expanded))
            if (expanded) addAll(sectionRows(JarnAppsSection.Hidden, sections.hidden, columns, titled = false))
        }
    }

internal fun List<JarnAppsRow>.focusOrder(): List<String> = flatMap { row ->
    when (row) {
        is JarnAppsRow.Apps -> row.apps.map { it.packageName }
        is JarnAppsRow.HiddenHeader -> listOf(row.key)
    }
}

internal fun List<JarnAppsRow>.rowIndexOf(key: String): Int = indexOfFirst { row ->
    when (row) {
        is JarnAppsRow.Apps -> row.apps.any { it.packageName == key }
        is JarnAppsRow.HiddenHeader -> row.key == key
    }
}

/** [key] if it survived, otherwise the nearest survivor after it, then before it. */
internal fun jarnAppsFocusAfter(previous: List<String>, next: List<String>, key: String): String? {
    if (key in next) return key
    val survivors = next.toHashSet()
    val index = previous.indexOf(key)
    if (index < 0) return next.firstOrNull()
    return previous.drop(index + 1).firstOrNull { it in survivors }
        ?: previous.take(index).lastOrNull { it in survivors }
        ?: next.firstOrNull()
}
