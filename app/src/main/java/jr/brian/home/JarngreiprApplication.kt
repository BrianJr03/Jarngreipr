package jr.brian.home

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp
import jr.brian.home.util.CrashLogger

@HiltAndroidApp
class JarngreiprApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.initialize(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
            else add(GifDecoder.Factory())
        }
        .build()
}
