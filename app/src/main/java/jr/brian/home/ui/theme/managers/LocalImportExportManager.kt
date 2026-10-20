package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.data.config.ImportExportManager

/**
 * Compose-side handle on the app-scoped [ImportExportManager]. Nullable
 * so composables measured inside a subcomposition that doesn't yet see
 * the activity's `CompositionLocalProvider` (observed in a Thor
 * config-change / re-measure race) can fall back to a Hilt lookup
 * instead of hard-crashing with `error("No ImportExportManager
 * provided")`.
 *
 * The non-null happy path is still what every normal composition
 * sees — `MainActivity.setContent` provides it above the entire nav
 * graph. The fallback is a defensive floor so a late re-measure can't
 * surface a hard crash. Readers should use the pattern:
 *
 * ```
 * val mgr = LocalImportExportManager.current ?: remember(context) {
 *     EntryPointAccessors.fromApplication(
 *         context.applicationContext,
 *         ImportExportManagerEntryPoint::class.java,
 *     ).importExportManager()
 * }
 * ```
 */
val LocalImportExportManager = compositionLocalOf<ImportExportManager?> { null }
