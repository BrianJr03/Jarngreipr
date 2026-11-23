package jr.brian.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.home.esde.scraper.jarn.AppJarnScraperSetup
import jr.brian.jarnfe.scraper.JarnScraperSetup
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JarnScraperModule {
    @Binds
    @Singleton
    abstract fun bindJarnScraperSetup(setup: AppJarnScraperSetup): JarnScraperSetup
}
