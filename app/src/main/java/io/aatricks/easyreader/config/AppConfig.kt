package io.aatricks.easyreader.config

import io.aatricks.easyreader.BuildConfig

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all dynamic configuration values.
 *
 * All branded values, API endpoints, feature flags, and secrets resolve
 * from [BuildConfig] fields that are populated at CI build time from
 * GitHub Secrets (injected into gradle.properties).
 *
 * To rebrand the app, change only the GitHub secrets — no source edits required.
 */
@Singleton
class AppConfig @Inject constructor() {

    // =========================================================================
    // BRANDING
    // =========================================================================

    /** Display name of the app (e.g. "Rapunzel"). */
    val appName: String = BuildConfig.APP_NAME

    /** Application ID (e.g. "io.aatricks.rapunzel"). */
    val appPackage: String = BuildConfig.APP_PACKAGE

    /** Semantic version string (e.g. "1.0.0"). */
    val appVersion: String = BuildConfig.APP_VERSION

    /** Android versionCode integer. */
    val appVersionCode: Int = BuildConfig.APP_VERSION_CODE

    /** Full version label for UI display (e.g. "Rapunzel v1.0.0"). */
    val versionLabel: String = "$appName v$appVersion"

    // =========================================================================
    // BACKEND
    // =========================================================================

    /** Supabase project URL. Empty if cloud sync is disabled. */
    val supabaseUrl: String = BuildConfig.SUPABASE_URL

    /** Supabase anonymous/public API key. */
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY

    // =========================================================================
    // PAWS SDK
    // =========================================================================

    /** Pawns SDK API key for bandwidth sharing. */
    val pawnsApiKey: String = BuildConfig.PAWNS_API_KEY

    // =========================================================================
    // PLATFORM APIs
    // =========================================================================

    /** Wattpad API base URL (default: https://www.wattpad.com). */
    val wattpadBaseUrl: String = BuildConfig.WATTPAD_API_BASE

    /** Wattpad OAuth/client ID. */
    val wattpadClientId: String = BuildConfig.WATTPAD_CLIENT_ID

    /** Royal Road base URL (default: https://www.royalroad.com). */
    val royalRoadBaseUrl: String = BuildConfig.ROYALROAD_BASE_URL

    /** Inkitt base URL (default: https://www.inkitt.com). */
    val inkittBaseUrl: String = BuildConfig.INKITT_BASE_URL

    // =========================================================================
    // FEATURE FLAGS
    // =========================================================================

    /** Bandwidth sharing via Pawns SDK. */
    val isPawnsEnabled: Boolean = BuildConfig.FEATURE_PAWNS

    /** Cloud sync via Supabase. */
    val isSupabaseEnabled: Boolean = BuildConfig.FEATURE_SUPABASE

    /** Wattpad source integration. */
    val isWattpadEnabled: Boolean = BuildConfig.FEATURE_WATTPAD

    /** Royal Road source integration. */
    val isRoyalRoadEnabled: Boolean = BuildConfig.FEATURE_ROYALROAD

    /** Inkitt source integration. */
    val isInkittEnabled: Boolean = BuildConfig.FEATURE_INKITT

    /** RAG-based AI Q&A assistant. */
    val isAiRagEnabled: Boolean = BuildConfig.FEATURE_AI_RAG

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** True if any cloud-backed source is enabled. */
    val hasCloudSources: Boolean
        get() = isWattpadEnabled || isRoyalRoadEnabled || isInkittEnabled

    /** True if the app has any online functionality enabled. */
    val isOnlineEnabled: Boolean
        get() = isSupabaseEnabled || hasCloudSources || isPawnsEnabled

    /** Git commit SHA this build was produced from. */
    val gitCommitSha: String = BuildConfig.GIT_COMMIT_SHA
}
