package jr.brian.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.frontend.library.migrate.EsdeAltEmulatorWriter
import jr.brian.home.esde.launch.AppEsdeAltEmulatorWriter
import jr.brian.home.esde.launch.AppJarnGameLauncher
import jr.brian.home.esde.launch.AppJarnGameOptions
import jr.brian.jarnfe.launch.JarnGameLauncher
import jr.brian.jarnfe.launch.JarnGameOptions
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JarnLaunchModule {
    @Binds
    @Singleton
    abstract fun bindJarnGameLauncher(launcher: AppJarnGameLauncher): JarnGameLauncher

    @Binds
    @Singleton
    abstract fun bindJarnGameOptions(options: AppJarnGameOptions): JarnGameOptions

    @Binds
    @Singleton
    abstract fun bindEsdeAltEmulatorWriter(writer: AppEsdeAltEmulatorWriter): EsdeAltEmulatorWriter
}
