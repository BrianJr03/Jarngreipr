package jr.brian.home.di

import android.content.Context
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageDecoderDecoder
import coil.decode.ImageSource
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.Options
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.media3.common.util.UnstableApi
import jr.brian.home.esde.data.ESDEEventListener
import jr.brian.home.esde.data.ESDEEventListenerImpl
import jr.brian.home.esde.data.ESDEEventManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.ui.video.VideoPresentationManager
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ESDEImageLoader

private class ExtensionAwareSvgDecoderFactory : Decoder.Factory {
    private val svgDecoderFactory = SvgDecoder.Factory()

    override fun create(
        result: SourceResult,
        options: Options,
        imageLoader: ImageLoader
    ): Decoder? {
        // Check if the file has an SVG extension by examining the source file path
        val isSvg = result.source.file().toFile().extension.equals("svg", ignoreCase = true)

        // If it's an SVG by extension, create the decoder directly
        if (isSvg) {
            return ExtensionAwareSvgDecoder(result.source, options)
        }

        // Otherwise, fall back to the default SVG decoder factory which checks MIME type
        return svgDecoderFactory.create(result, options, imageLoader)
    }
}

/**
 * Wrapper decoder that delegates to SvgDecoder for actual SVG decoding.
 */
private class ExtensionAwareSvgDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {
    override suspend fun decode(): DecodeResult {
        // Use the standard SvgDecoder for actual decoding
        return SvgDecoder(source, options).decode()
    }
}

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
                add(ExtensionAwareSvgDecoderFactory())
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

    @UnstableApi
    @Provides
    @Singleton
    fun provideVideoPresentationManager(): VideoPresentationManager = VideoPresentationManager
}