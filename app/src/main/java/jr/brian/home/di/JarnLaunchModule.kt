package jr.brian.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.home.esde.launch.AppJarnGameLauncher
import jr.brian.jarnfe.launch.JarnGameLauncher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JarnLaunchModule {
    @Binds
    @Singleton
    abstract fun bindJarnGameLauncher(launcher: AppJarnGameLauncher): JarnGameLauncher
}
