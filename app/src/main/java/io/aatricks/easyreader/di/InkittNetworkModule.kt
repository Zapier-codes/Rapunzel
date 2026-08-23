package io.aatricks.easyreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.remote.inkitt.InkittApiService
import io.aatricks.easyreader.data.remote.inkitt.InkittAuthInterceptor
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
object InkittNetworkModule {

    private const val NETWORK_TIMEOUT_SECONDS = 30L

    // InkittAuthInterceptor is constructor-injected (@Inject constructor); no manual
    // @Provides binding here to avoid a duplicate Dagger binding.

    @Provides
    @Singleton
    fun provideInkittOkHttpClient(
        baseClient: OkHttpClient,
        authInterceptor: InkittAuthInterceptor,
    ): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideInkittRetrofit(
        appConfig: AppConfig,
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(appConfig.inkittBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideInkittApiService(retrofit: Retrofit): InkittApiService {
        return retrofit.create(InkittApiService::class.java)
    }
}
