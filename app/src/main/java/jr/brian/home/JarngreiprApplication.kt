package jr.brian.home

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp
import jr.brian.home.data.HomeButtonManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.launch.AppJarnSystemManager
import jr.brian.home.service.HomeInterceptionKeepAliveService
import jr.brian.home.util.CrashLogger
import jr.brian.home.util.ThorDetection
import jr.brian.home.canvas.jarn.AppJarnCanvasData
import jr.brian.home.canvas.jarn.JarnTileRemovalRecovery
import jr.brian.home.jarn.AppJarnMeetJarn
import jr.brian.home.jarn.AppJarnAppsData
import jr.brian.home.jarn.AppJarnBackup
import jr.brian.home.jarn.AppJarnDockData
import jr.brian.home.jarn.AppJarnInputTools
import jr.brian.home.jarn.AppJarnExperienceSwitch
import jr.brian.home.jarn.AppJarnHomeApp
import jr.brian.home.jarn.AppJarnLauncherSettings
import jr.brian.home.jarn.AppJarnScreenSwap
import jr.brian.home.jarn.AppJarnUpdates
import jr.brian.home.jarn.AppJarnUtilities
import jr.brian.home.jarn.AppJarnWallpaperSource
import jr.brian.jarnfe.apps.JarnAppsData
import jr.brian.jarnfe.apps.JarnAppsDataProvider
import jr.brian.jarnfe.backup.JarnBackup
import jr.brian.jarnfe.backup.JarnDataProvider
import jr.brian.jarnfe.backup.JarnUpdates
import jr.brian.jarnfe.dock.JarnDockData
import jr.brian.jarnfe.dock.JarnDockDataProvider
import jr.brian.jarnfe.shell.JarnExperienceSwitch
import jr.brian.jarnfe.shell.JarnExperienceSwitchProvider
import jr.brian.jarnfe.shell.JarnScreenSwap
import jr.brian.jarnfe.shell.JarnScreenSwapProvider
import jr.brian.jarnfe.wallpaper.JarnWallpaperProvider
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import jr.brian.jarnfe.canvas.JarnCanvasData
import jr.brian.jarnfe.canvas.JarnCanvasDataProvider
import jr.brian.jarnfe.launch.JarnGameLauncher
import jr.brian.jarnfe.launch.JarnGameLauncherProvider
import jr.brian.jarnfe.launch.JarnGameOptions
import jr.brian.jarnfe.launch.JarnGameOptionsProvider
import jr.brian.jarnfe.launch.JarnPlaySessionsProvider
import jr.brian.jarnfe.home.JarnHomeApp
import jr.brian.jarnfe.home.JarnHomeAppProvider
import jr.brian.jarnfe.library.JarnArtDetectorProvider
import jr.brian.jarnfe.library.JarnEsdeRomsProvider
import jr.brian.jarnfe.library.JarnGameVideos
import jr.brian.jarnfe.library.JarnGameVideosProvider
import jr.brian.jarnfe.library.JarnLibraryDataProvider
import jr.brian.jarnfe.look.JarnLookStoreProvider
import jr.brian.jarnfe.settings.JarnLauncherSettings
import jr.brian.jarnfe.settings.JarnLauncherSettingsProvider
import jr.brian.jarnfe.settings.JarnMeetJarn
import jr.brian.jarnfe.settings.JarnMeetJarnProvider
import jr.brian.jarnfe.systems.JarnSystemManager
import jr.brian.jarnfe.systems.JarnSystemManagerProvider
import jr.brian.jarnfe.scraper.JarnScraperSetup
import jr.brian.jarnfe.scraper.JarnScraperSetupProvider
import jr.brian.jarnfe.tools.JarnInputTools
import jr.brian.jarnfe.tools.JarnInputToolsProvider
import jr.brian.home.jarn.AppJarnGameVideos
import jr.brian.home.jarn.AppJarnRssData
import jr.brian.jarnfe.rss.JarnRssData
import jr.brian.jarnfe.rss.JarnRssDataProvider
import jr.brian.jarnfe.utilities.JarnUtilities
import jr.brian.jarnfe.utilities.JarnUtilitiesProvider
import jr.brian.frontend.library.art.ArtSourceDetector
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.play.PlaySessionRecorder
import jr.brian.frontend.library.setup.EsdeRomsFolderFinder
import jr.brian.frontend.library.setup.EsdeRomsFolderSource
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.frontend.look.JarnLookStore
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class JarngreiprApplication :
    Application(),
    ImageLoaderFactory,
    Configuration.Provider,
    JarnGameLauncherProvider,
    JarnGameOptionsProvider,
    JarnPlaySessionsProvider,
    JarnLibraryDataProvider,
    JarnCanvasDataProvider,
    JarnLookStoreProvider,
    JarnScreenSwapProvider,
    JarnExperienceSwitchProvider,
    JarnWallpaperProvider,
    JarnDockDataProvider,
    JarnAppsDataProvider,
    JarnSystemManagerProvider,
    JarnScraperSetupProvider,
    JarnInputToolsProvider,
    JarnDataProvider,
    JarnUtilitiesProvider,
    JarnRssDataProvider,
    JarnGameVideosProvider,
    JarnLauncherSettingsProvider,
    jr.brian.jarnfe.settings.JarnGamelistSettingProvider,
    jr.brian.jarnfe.audio.JarnAudioDataProvider,
    JarnArtDetectorProvider,
    JarnEsdeRomsProvider,
    JarnHomeAppProvider,
    JarnMeetJarnProvider,
    jr.brian.jarnfe.themes.JarnThemeImportProvider {

    /** [Provider]s so Classic never builds Jarn FE's theme import. */
    @Inject
    lateinit var jarnEsdeThemeLocatorProvider: Provider<jr.brian.frontend.look.themes.EsdeThemeLocator>

    override val jarnEsdeThemeLocator: jr.brian.frontend.look.themes.EsdeThemeLocator
        get() = jarnEsdeThemeLocatorProvider.get()

    @Inject
    lateinit var jarnEsdeThemeUpdatesProvider: Provider<jr.brian.frontend.look.themes.EsdeThemeUpdates>

    override val jarnEsdeThemeUpdates: jr.brian.frontend.look.themes.EsdeThemeUpdates
        get() = jarnEsdeThemeUpdatesProvider.get()

    @Inject
    lateinit var jarnThemeFileImporterProvider: Provider<jr.brian.frontend.look.themes.JarnThemeImporter>

    override val jarnThemeFileImporter: jr.brian.frontend.look.themes.JarnThemeImporter
        get() = jarnThemeFileImporterProvider.get()

    @Inject
    lateinit var jarnSystemCatalogProvider: Provider<jr.brian.frontend.library.systems.SystemCatalog>

    override val jarnSystemCatalog: jr.brian.frontend.library.systems.SystemCatalog
        get() = jarnSystemCatalogProvider.get()

    @Inject
    lateinit var jarnMeetJarnProvider: Provider<AppJarnMeetJarn>

    override val jarnMeetJarn: JarnMeetJarn
        get() = jarnMeetJarnProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's library data. */
    @Inject
    lateinit var jarnLibraryDataProvider: Provider<JarnLibraryData>

    override val jarnLibraryData: JarnLibraryData
        get() = jarnLibraryDataProvider.get()

    @Inject
    lateinit var jarnLibrarySetupProvider: Provider<LibrarySetup>

    override val jarnLibrarySetup: LibrarySetup
        get() = jarnLibrarySetupProvider.get()

    @Inject
    lateinit var jarnEsdeRomsFolderProvider: Provider<EsdeRomsFolderFinder>

    override val jarnEsdeRomsFolder: EsdeRomsFolderSource
        get() = jarnEsdeRomsFolderProvider.get()

    /** A [Provider] so Classic never runs Jarn FE's art detection. */
    @Inject
    lateinit var jarnArtDetectorProvider: Provider<ArtSourceDetector>

    override val jarnArtDetector: ArtSourceDetector
        get() = jarnArtDetectorProvider.get()

    /** A [Provider] so nothing launch-related is built until Jarn FE asks for it. */
    @Inject
    lateinit var jarnGameLauncherProvider: Provider<JarnGameLauncher>

    override val jarnGameLauncher: JarnGameLauncher
        get() = jarnGameLauncherProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's play tracking. */
    @Inject
    lateinit var jarnPlaySessionsProvider: Provider<PlaySessionRecorder>

    override val jarnPlaySessions: PlaySessionRecorder
        get() = jarnPlaySessionsProvider.get()

    @Inject
    lateinit var jarnGameOptionsProvider: Provider<JarnGameOptions>

    override val jarnGameOptions: JarnGameOptions
        get() = jarnGameOptionsProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's canvas bridge. */
    @Inject
    lateinit var jarnCanvasDataProvider: Provider<AppJarnCanvasData>

    override val jarnCanvasData: JarnCanvasData
        get() = jarnCanvasDataProvider.get()

    /** A [Provider] so Classic never reads Jarn FE's appearance. */
    @Inject
    lateinit var jarnLookStoreProvider: Provider<JarnLookStore>

    override val jarnLookStore: JarnLookStore
        get() = jarnLookStoreProvider.get()
    @Inject
    lateinit var jarnScreenSwapProvider: Provider<AppJarnScreenSwap>

    override val jarnScreenSwap: JarnScreenSwap
        get() = jarnScreenSwapProvider.get()

    @Inject
    lateinit var jarnExperienceSwitchProvider: Provider<AppJarnExperienceSwitch>

    override val jarnExperienceSwitch: JarnExperienceSwitch
        get() = jarnExperienceSwitchProvider.get()

    @Inject
    lateinit var jarnWallpaperSourceProvider: Provider<AppJarnWallpaperSource>

    override val jarnWallpaperSource: JarnWallpaperSource
        get() = jarnWallpaperSourceProvider.get()

    @Inject
    lateinit var jarnDockDataProvider: Provider<AppJarnDockData>

    override val jarnDockData: JarnDockData
        get() = jarnDockDataProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's Apps drawer data. */
    @Inject
    lateinit var jarnAppsDataProvider: Provider<AppJarnAppsData>

    override val jarnAppsData: JarnAppsData
        get() = jarnAppsDataProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's system manager. */
    @Inject
    lateinit var jarnSystemManagerProvider: Provider<AppJarnSystemManager>

    override val jarnSystemManager: JarnSystemManager
        get() = jarnSystemManagerProvider.get()
    /** A [Provider] so Classic never builds Jarn FE's scraper setup. */
    @Inject
    lateinit var jarnScraperSetupProvider: Provider<JarnScraperSetup>

    override val jarnScraperSetup: JarnScraperSetup
        get() = jarnScraperSetupProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's input tools. */
    @Inject
    lateinit var jarnInputToolsProvider: Provider<AppJarnInputTools>

    override val jarnInputTools: JarnInputTools
        get() = jarnInputToolsProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's backup screens. */
    @Inject
    lateinit var jarnBackupProvider: Provider<AppJarnBackup>

    override val jarnBackup: JarnBackup
        get() = jarnBackupProvider.get()

    @Inject
    lateinit var jarnUpdatesProvider: Provider<AppJarnUpdates>

    override val jarnUpdates: JarnUpdates
        get() = jarnUpdatesProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's utility screens. */
    @Inject
    lateinit var jarnUtilitiesProvider: Provider<AppJarnUtilities>

    override val jarnUtilities: JarnUtilities
        get() = jarnUtilitiesProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's launcher settings. */
    @Inject
    lateinit var jarnHomeAppProvider: Provider<AppJarnHomeApp>

    override val jarnHomeApp: JarnHomeApp
        get() = jarnHomeAppProvider.get()

    @Inject
    lateinit var jarnLauncherSettingsProvider: Provider<AppJarnLauncherSettings>

    override val jarnLauncherSettings: JarnLauncherSettings
        get() = jarnLauncherSettingsProvider.get()

    @Inject
    lateinit var jarnGamelistSettingProvider: Provider<jr.brian.home.jarn.AppJarnGamelistSetting>

    override val jarnGamelistSetting: jr.brian.jarnfe.settings.JarnGamelistSetting
        get() = jarnGamelistSettingProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's RSS bridge. */
    @Inject
    lateinit var jarnRssDataProvider: Provider<AppJarnRssData>

    override val jarnRssData: JarnRssData
        get() = jarnRssDataProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's game previews. */
    @Inject
    lateinit var jarnGameVideosProvider: Provider<AppJarnGameVideos>

    override val jarnGameVideos: JarnGameVideos
        get() = jarnGameVideosProvider.get()

    /** A [Provider] so Classic never builds Jarn FE's audio bridge. */
    @Inject
    lateinit var jarnAudioDataProvider: Provider<jr.brian.home.jarn.AppJarnAudioData>

    override val jarnAudioData: jr.brian.jarnfe.audio.JarnAudioData
        get() = jarnAudioDataProvider.get()

    /**
     * Injected as [Provider] so Dagger does NOT construct the singleton as a
     * side effect of Application injection — the heavy `loadState()` work in
     * [ESDEPreferencesManager]'s initializer would otherwise run on the main
     * thread before `onCreate` even gets to kick off a background warmup.
     */
    @Inject
    lateinit var esdePreferencesManagerProvider: Provider<ESDEPreferencesManager>

    @Inject
    lateinit var homeButtonManager: HomeButtonManager

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var screenSwapListener: jr.brian.home.data.ScreenSwapListener

    @Inject
    lateinit var screenAssignmentStore: jr.brian.home.data.ScreenAssignmentStore

    @Inject
    lateinit var sourcesSyncService: jr.brian.home.data.SourcesSyncService

    @Inject
    lateinit var tileRemovalRecovery: JarnTileRemovalRecovery

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CrashLogger.initialize(this)
        restoreHomeInterceptionKeepAlive()
        prewarmSingletons()
        // Phase 5: wire FrontendCommand.SwapScreens → persisted
        // assignment flip + re-route. Kept alive for the lifetime of
        // the Application.
        screenSwapListener.start()
        sourcesSyncService.start()
        tileRemovalRecovery.start()
        // Reconcile AYN's Focus Lock to the current screen assignment
        // on boot. No-op without Shizuku or when the toggle is off.
        jr.brian.home.data.ScreenFocusLockWriter.syncToAssignment(
            this,
            screenAssignmentStore.assignment,
        )
    }

    /**
     * The keep-alive foreground service is normally started/stopped only when the
     * user toggles the "Home Button Interception" setting
     * ([jr.brian.home.ui.components.settings.HomeInterceptionSettingItem]). After a
     * force-stop that service is killed, and without this restore nothing would
     * bring it back until the user manually toggled the setting off/on. The
     * accessibility service's rebind is noticeably more reliable while the
     * keep-alive is running, so start it eagerly whenever the persisted flag says
     * interception is on.
     */
    private fun restoreHomeInterceptionKeepAlive() {
        if (!ThorDetection.isThor()) return
        if (!homeButtonManager.interceptionEnabled.value) return
        HomeInterceptionKeepAliveService.start(this)
    }

    /**
     * Fire eager background construction of singletons whose first-access cost
     * would otherwise land on the main thread of whichever Activity injects
     * them first. [ESDEPreferencesManager] in particular reads 50+ SharedPrefs
     * keys and decodes ~15 JSON blobs on first touch — before this prewarm,
     * launching the frontend as the first activity on a cold start could stall
     * for seconds while that ran synchronously under Hilt field injection.
     *
     * A `@Singleton` provision is lock-synchronized inside Dagger, so if a
     * foreground injection races this thread, the foreground injection simply
     * blocks on the same construction rather than running a parallel load.
     */
    private fun prewarmSingletons() {
        Thread({
            runCatching { esdePreferencesManagerProvider.get() }
            runCatching { jr.brian.home.esde.util.BundledEsdeSystems.prewarm(this) }
        }, "jarngreipr-prewarm").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
            start()
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(ImageDecoderDecoder.Factory()) }
        .build()
}
