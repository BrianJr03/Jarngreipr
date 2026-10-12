package jr.brian.home.esde.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.home.esde.scraper.ScraperRateLimiter
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScraperModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @Named(SCREENSCRAPER_LIMITER)
    fun provideScreenScraperRateLimiter(): ScraperRateLimiter =
        ScraperRateLimiter(initialThreads = 1, initialMinIntervalMs = 1200L)

    @Provides
    @Singleton
    @Named(STEAMGRIDDB_LIMITER)
    fun provideSteamGridDbRateLimiter(): ScraperRateLimiter =
        ScraperRateLimiter(initialThreads = 1, initialMinIntervalMs = 500L)

    const val SCREENSCRAPER_LIMITER = "screenScraperRateLimiter"
    const val STEAMGRIDDB_LIMITER = "steamGridDbRateLimiter"
}
