package jr.brian.core.model.bus

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the single [FrontendBus] implementation. Lives in :core-model so
 * every module can @Inject the interface without pulling the adapter's
 * backing fields into their classpath.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FrontendBusModule {
    @Binds
    @Singleton
    abstract fun bindFrontendBus(adapter: FrontendBusAdapter): FrontendBus
}
