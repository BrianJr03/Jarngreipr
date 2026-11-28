package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.jarnfe.library.withSystemsBelowHeader
import jr.brian.jarnfe.themes.JarnImportedThemes
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeKind
import jr.brian.jarnfe.themes.JarnUserThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Built-ins and templates live in code, so they can only be copied, never edited in place. */
internal val JarnThemeEntry.editableInPlace: Boolean
    get() = kind != JarnThemeKind.BuiltIn && kind != JarnThemeKind.Template

/** The theme [entry] as the editor should open it: its draft when there is one, else the saved theme. */
internal fun JarnUserThemeStore.editableSpec(entry: JarnThemeEntry): JarnThemeSpec =
    draft(entry.id) ?: theme(entry.id) ?: entry.theme.spec

/**
 * A new theme copied from [entry], named [name], saved right away so it is listed as a layout from
 * the start. A built-in's list moves below the header band, since a copy shows the header as its
 * own strip. It follows the Look unless [entry] has its own colors, and keeps the source and
 * license of what it was copied from.
 */
internal fun JarnUserThemeStore.duplicate(entry: JarnThemeEntry, name: String): Pair<String, JarnThemeSpec> {
    val base = entry.theme.spec.let { if (entry.theme.pins.isEmpty()) it else it.withSystemsBelowHeader() }
    val spec = base.copy(meta = base.meta.copy(name = name, version = null, followsLook = !entry.ownColors))
    val id = newId(name)
    save(id, spec)
    return id to spec
}

/** Opens [entry] in the editor: in place when it is a theme on this device, else as a copy named [copyName]. */
internal suspend fun openThemeEditor(store: JarnUserThemeStore, entry: JarnThemeEntry, copyName: String?) {
    val (id, spec) = withContext(Dispatchers.IO) {
        if (copyName == null && entry.editableInPlace) entry.id to store.editableSpec(entry) else store.duplicate(entry, copyName ?: entry.id)
    }
    JarnImportedThemes.refresh(store.root)
    JarnThemeEditor.open(id, spec, store)
}

/**
 * The name for a new copy: [first] ("Grid copy") while no theme uses it, then [numbered] from 2
 * ("Grid copy 2", "Grid copy 3"), so every copy can be told apart on its card.
 */
internal fun nextCopyName(first: String, numbered: (Int) -> String, taken: Set<String>): String =
    if (first !in taken) first else generateSequence(2) { it + 1 }.map(numbered).first { it !in taken }

private const val NAME_MARK = "\u0000"
private const val NUMBER_MARK = 987_654_321

/**
 * [name] without a copy ending made by [plain] ("%1$s copy") or [numbered] ("%1$s copy %2$d"), so
 * copying "Grid copy" makes "Grid copy 2", not "Grid copy copy". It reads the endings from the
 * strings themselves, so it follows any translation.
 */
internal fun copyBaseName(name: String, plain: (String) -> String, numbered: (String, Int) -> String): String {
    val templates = listOf(plain(NAME_MARK), numbered(NAME_MARK, NUMBER_MARK))
    return templates.firstNotNullOfOrNull { copyPattern(it).matchEntire(name)?.groupValues?.get(1) } ?: name
}

/** A template with its name mark as a capture and any number mark as digits, the rest literal. */
private fun copyPattern(template: String): Regex {
    val pattern = template.split(NAME_MARK).joinToString("(.+?)") { part ->
        part.split(NUMBER_MARK.toString()).joinToString("\\d+") { Regex.escape(it) }
    }
    return Regex(pattern)
}
