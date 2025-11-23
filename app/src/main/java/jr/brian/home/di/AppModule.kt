package jr.brian.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.AppVisibilityManager
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
}
