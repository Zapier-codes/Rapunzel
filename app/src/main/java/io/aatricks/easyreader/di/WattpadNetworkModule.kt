package io.aatricks.easyreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.remote.wattpad.WattpadApiService
import io.aatricks.easyreader.data.remote.wattpad.WattpadAuthInterceptor
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WattpadNetworkModule {

    @Provides
    @Singleton
    fun provideWattpadAuthInterceptor(appConfig: AppConfig): WattpadAuthInterceptor {
        return WattpadAuthInterceptor(appConfig)
    }

    @Provides
    @Singleton
    fun provideWattpadOkHttpClient(
        baseClient: OkHttpClient,
        authInterceptor: WattpadAuthInterceptor,
    ): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWattpadRetrofit(
        appConfig: AppConfig,
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(appConfig.wattpadBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideWattpadApiService(retrofit: Retrofit): WattpadApiService {
        return retrofit.create(WattpadApiService::class.java)
    }
}
