package jr.brian.frontend.library

/**
 * Observable state of the library engine. [LibraryRepository.state] drives
 * UI chrome ("Scanning: 3 / 12 systems") and surfaces failures end users
 * would otherwise have to find in logcat.
 *
 * `Scanning` carries counts in the same shape the Phase 0 fixes use on
 * `RomSearchStateHolder.systemScanLoaded` / `systemScanTotal`, so the
 * Phase 3 UI binding is a near-rename rather than a rework.
 */
sealed interface LibraryState {
    data object Idle : LibraryState

    data class Scanning(
        val systemsDone: Int,
        val systemsTotal: Int,
        val currentSystemId: String? = null,
    ) : LibraryState

    data class Error(val message: String) : LibraryState
}
