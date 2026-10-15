package jr.brian.frontend.scrape

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt graph for the Phase 4 scraper. All providers are @Named so
 * :frontend doesn't collide with :app's existing OkHttpClient /
 * Json providers in `jr.brian.home.esde.di.ScraperModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
object FrontendScraperModule {

    const val OKHTTP = "frontendScraperOkHttp"
    const val JSON = "frontendScraperJson"
    const val SCREENSCRAPER_LIMITER = "frontendScreenScraperRateLimiter"
    const val STEAMGRIDDB_LIMITER = "frontendSteamGridDbRateLimiter"

    @Provides
    @Singleton
    @Named(OKHTTP)
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    @Named(JSON)
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
}
