package jr.brian.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.home.esde.events.ESDEEventListener
import jr.brian.home.esde.events.ESDEEventListenerImpl
import jr.brian.home.esde.events.ESDEEventManager
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ESDEModule {

    @Provides
    @Singleton
    fun provideESDEPreferencesManager(
        @ApplicationContext context: Context
    ): ESDEPreferencesManager {
        return ESDEPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideESDEEventListenerImpl(): ESDEEventListenerImpl {
        return ESDEEventListenerImpl()
    }

    @Provides
    @Singleton
    fun provideESDEEventListener(
        impl: ESDEEventListenerImpl
    ): ESDEEventListener {
        return impl
    }

    @Provides
    @Singleton
    fun provideESDEEventManager(
        eventListener: ESDEEventListener
    ): ESDEEventManager {
        return ESDEEventManager(eventListener)
    }
}