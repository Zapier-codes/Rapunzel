package io.aatricks.easyreader.data.remote.wattpad

import io.aatricks.easyreader.config.AppConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit interceptor that injects Wattpad auth token into every request.
 * Token is obtained via login flow and stored in encrypted preferences.
 */
@Singleton
class WattpadAuthInterceptor @Inject constructor(
    private val appConfig: AppConfig,
) : Interceptor {

    @Volatile
    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    fun clearToken() {
        authToken = null
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "${appConfig.appName}/${appConfig.appVersion}")

        authToken?.let {
            builder.header("Authorization", "Bearer $it")
        }

        return chain.proceed(builder.build())
    }
}
