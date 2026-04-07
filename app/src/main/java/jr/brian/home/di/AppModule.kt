package jr.brian.home.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppPositionManager
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.ControlPadManager
import jr.brian.home.data.CustomIconManager
import jr.brian.home.data.DockManager
import jr.brian.home.data.FloatyModeManager
import jr.brian.home.data.GameKonfettiManager
import jr.brian.home.data.QuickDeleteManager
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.IconPackManager
import jr.brian.home.data.OnboardingManager
import jr.brian.home.data.PageCountManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.PowerSettingsManager
import jr.brian.home.data.CustomAppNameManager
import jr.brian.home.data.SearchLayoutManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.data.WidgetProviderRepository
import jr.brian.home.data.WhatsNewManager
import jr.brian.home.data.RssRepository
import jr.brian.home.data.database.AppDatabase
import jr.brian.home.data.database.CustomIconDao
import jr.brian.home.data.database.RssFeedDao
import jr.brian.home.ui.theme.managers.WallpaperManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppVisibilityManager(
        @ApplicationContext context: Context
    ): AppVisibilityManager {
        return AppVisibilityManager(context)
    }

    @Provides
    @Singleton
    fun provideWidgetPreferences(
        @ApplicationContext context: Context
    ): WidgetPreferences {
        return WidgetPreferences(context)
    }

    @Provides
    @Singleton
    fun provideGridSettingsManager(
        @ApplicationContext context: Context
    ): GridSettingsManager {
        return GridSettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideAppDisplayPreferenceManager(
        @ApplicationContext context: Context
    ): AppDisplayPreferenceManager {
        return AppDisplayPreferenceManager(context)
    }

    @Provides
    @Singleton
    fun providePowerSettingsManager(
        @ApplicationContext context: Context
    ): PowerSettingsManager {
        return PowerSettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideWidgetPageAppManager(
        @ApplicationContext context: Context
    ): WidgetPageAppManager {
        return WidgetPageAppManager(context)
    }

    @Provides
    @Singleton
    fun provideHomeTabManager(
        @ApplicationContext context: Context
    ): HomeTabManager {
        return HomeTabManager(context)
    }

    @Provides
    @Singleton
    fun provideOnboardingManager(
        @ApplicationContext context: Context
    ): OnboardingManager {
        return OnboardingManager(context)
    }

    @Provides
    @Singleton
    fun provideQuickDeleteManager(
        @ApplicationContext context: Context
    ): QuickDeleteManager {
        return QuickDeleteManager(context)
    }

    @Provides
    @Singleton
    fun provideAppPositionManager(
        @ApplicationContext context: Context
    ): AppPositionManager {
        return AppPositionManager(context)
    }

    @Provides
    @Singleton
    fun providePageCountManager(
        @ApplicationContext context: Context
    ): PageCountManager {
        return PageCountManager(context)
    }

    @Provides
    @Singleton
    fun providePageTypeManager(
        @ApplicationContext context: Context
    ): PageTypeManager {
        return PageTypeManager(context)
    }

    @Provides
    @Singleton
    fun provideIconPackManager(
        @ApplicationContext context: Context
    ): IconPackManager {
        return IconPackManager(context)
    }

    @Provides
    @Singleton
    fun provideWhatsNewManager(
        @ApplicationContext context: Context
    ): WhatsNewManager {
        return WhatsNewManager(context)
    }

    @Provides
    @Singleton
    fun provideGameKonfettiManager(
        @ApplicationContext context: Context
    ): GameKonfettiManager {
        return GameKonfettiManager(context)
    }

    @Provides
    @Singleton
    fun provideFloatyModeManager(
        @ApplicationContext context: Context
    ): FloatyModeManager {
        return FloatyModeManager(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jarngreipr_database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideCustomIconDao(database: AppDatabase): CustomIconDao {
        return database.customIconDao()
    }

    @Provides
    @Singleton
    fun provideRssFeedDao(database: AppDatabase): RssFeedDao {
        return database.rssFeedDao()
    }

    @Provides
    @Singleton
    fun provideRssRepository(rssFeedDao: RssFeedDao): RssRepository {
        return RssRepository(rssFeedDao)
    }

    @Provides
    @Singleton
    fun provideCustomIconManager(
        @ApplicationContext context: Context,
        customIconDao: CustomIconDao
    ): CustomIconManager {
        return CustomIconManager(context, customIconDao)
    }

    @Provides
    @Singleton
    fun provideWidgetProviderRepository(
        @ApplicationContext context: Context
    ): WidgetProviderRepository {
        return WidgetProviderRepository(context)
    }

    @Provides
    @Singleton
    fun provideControlPadManager(
        @ApplicationContext context: Context
    ): ControlPadManager {
        return ControlPadManager(context)
    }

    @Provides
    @Singleton
    fun provideDockManager(
        @ApplicationContext context: Context
    ): DockManager {
        return DockManager(context)
    }

    @Provides
    @Singleton
    fun provideWallpaperManager(
        @ApplicationContext context: Context
    ): WallpaperManager {
        return WallpaperManager(context)
    }

    @Provides
    @Singleton
    fun provideSearchLayoutManager(
        @ApplicationContext context: Context
    ): SearchLayoutManager {
        return SearchLayoutManager(context)
    }

    @Provides
    @Singleton
    fun provideCustomAppNameManager(
        @ApplicationContext context: Context
    ): CustomAppNameManager {
        return CustomAppNameManager(context)
    }
}
