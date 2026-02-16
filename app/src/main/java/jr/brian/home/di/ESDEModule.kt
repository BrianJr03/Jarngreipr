package jr.brian.home.di

import android.content.Context
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.home.esde.events.ESDEEventListener
import jr.brian.home.esde.events.ESDEEventListenerImpl
import jr.brian.home.esde.events.ESDEEventManager
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.setup.SetupPreferences
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ESDEImageLoader

@Module
@InstallIn(SingletonComponent::class)
object ESDEModule {

    @Provides
    @Singleton
    @ESDEImageLoader
    fun provideESDEImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.30) // Use 30% of available memory for ESDE images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("esde_image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Always cache local files
            .build()
    }

    @Provides
    @Singleton
    fun provideSetupPreferences(
        @ApplicationContext context: Context
    ): SetupPreferences {
        return SetupPreferences(context)
    }

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