package io.aatricks.easyreader.data.remote.supabase

import io.aatricks.easyreader.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase client wrapper.
 * Initialized only when Supabase feature flag is enabled and credentials are present.
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    private val appConfig: AppConfig,
) {
    val client: SupabaseClient? by lazy {
        if (!appConfig.isSupabaseEnabled || appConfig.supabaseUrl.isBlank() || appConfig.supabaseAnonKey.isBlank()) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = appConfig.supabaseUrl,
                supabaseKey = appConfig.supabaseAnonKey,
            ) {
                install(Postgrest)
                install(Auth)
                install(Realtime)
            }
        }
    }

    val isAvailable: Boolean
        get() = client != null
}
