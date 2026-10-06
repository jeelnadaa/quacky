package app.quacky.core.capability

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CapabilityModule {

    @Binds
    @Singleton
    abstract fun bindDeviceCapabilities(
        impl: AndroidDeviceCapabilities
    ): DeviceCapabilities
}
