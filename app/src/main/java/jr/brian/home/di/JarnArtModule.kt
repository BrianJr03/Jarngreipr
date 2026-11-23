package jr.brian.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.frontend.library.art.EsdeMediaRootProvider
import jr.brian.frontend.library.art.LibraryVisibility
import jr.brian.frontend.library.art.LibraryVisibilitySource
import jr.brian.frontend.library.art.RecentLaunch
import jr.brian.frontend.library.art.RecentLaunchSource
import jr.brian.frontend.library.setup.LibraryScanPreparer
import jr.brian.home.data.LibraryStoreSync
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.SetupPreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Provider

private const val ANDROID_APPS_SYSTEM = "androidapps"

/** Hands Jarn FE's data layer the values only `:app` knows. */
@Module
@InstallIn(SingletonComponent::class)
object JarnArtModule {

    @Provides
    fun provideEsdeMediaRoot(
        prefs: ESDEPreferencesManager,
        setup: SetupPreferences,
    ): EsdeMediaRootProvider = EsdeMediaRootProvider {
        (prefs.state.value.customMediaPath ?: setup.mediaPath)
            .takeIf { it.isNotBlank() }
            ?.let(::File)
    }

    /** Classic's hide rules, plus its Android apps exclusion from the systems list. */
    @Provides
    fun provideVisibility(prefs: ESDEPreferencesManager): LibraryVisibilitySource =
        LibraryVisibilitySource {
            prefs.state
                .map { LibraryVisibility(it.hiddenSystems + ANDROID_APPS_SYSTEM, it.hiddenGames) }
                .distinctUntilChanged()
        }

    @Provides
    fun provideScanPreparer(sync: Provider<LibraryStoreSync>): LibraryScanPreparer =
        LibraryScanPreparer { sync.get().prepareForScan() }

    @Provides
    fun provideRecentLaunches(manager: RecentlyPlayedManager): RecentLaunchSource =
        RecentLaunchSource {
            manager.entries.map { entries ->
                entries.map { RecentLaunch(it.rom.systemName, it.rom.path, it.launchedAt) }
            }
        }
}
