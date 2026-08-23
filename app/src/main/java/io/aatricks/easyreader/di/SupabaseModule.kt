package io.aatricks.easyreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.remote.supabase.SupabaseClientProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClientProvider(appConfig: AppConfig): SupabaseClientProvider {
        return SupabaseClientProvider(appConfig)
    }

    // SupabaseRepository is constructor-injected (@Inject constructor); no manual
    // @Provides binding here to avoid a duplicate Dagger binding.
}
