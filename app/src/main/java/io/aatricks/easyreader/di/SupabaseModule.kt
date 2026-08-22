package io.aatricks.easyreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.remote.supabase.SupabaseClientProvider
import io.aatricks.easyreader.data.remote.supabase.SupabaseRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClientProvider(appConfig: AppConfig): SupabaseClientProvider {
        return SupabaseClientProvider(appConfig)
    }

    @Provides
    @Singleton
    fun provideSupabaseRepository(
        appConfig: AppConfig,
        clientProvider: SupabaseClientProvider,
    ): SupabaseRepository {
        return SupabaseRepository(appConfig, clientProvider)
    }
}
