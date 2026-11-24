package jr.brian.core.model

import javax.inject.Qualifier

/**
 * Hilt qualifier for the application-scoped supervisor [kotlinx.coroutines.CoroutineScope].
 *
 * Use it to request a scope that survives the activity that triggered the
 * work finishing — the canonical case is the library scan: `RomGameLauncher`
 * calls `activity.finish()` after every game launch, which cancels any
 * `viewModelScope` the scan started on. Code that must keep running past
 * that `finish()` injects `@ApplicationScope CoroutineScope`.
 *
 * The provider lives in `:app`'s `AppModule`; the qualifier lives here so
 * that `:frontend` (and any other library module) can declare the same
 * dependency without importing `:app`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
