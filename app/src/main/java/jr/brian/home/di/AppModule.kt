package jr.brian.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppPositionManager
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.QuickDeleteManager
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.OnboardingManager
import jr.brian.home.data.PowerSettingsManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.data.WidgetPreferences
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
}
