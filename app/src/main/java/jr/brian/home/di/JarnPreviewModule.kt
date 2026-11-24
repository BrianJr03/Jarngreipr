package jr.brian.home.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.preview.JarnPreviewGate
import jr.brian.home.data.preview.JarnPreviewStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class JarnPreviewModule {
    @Binds
    @Singleton
    abstract fun bindJarnPreviewGate(store: JarnPreviewStore): JarnPreviewGate
}
