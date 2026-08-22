package io.aatricks.easyreader.data.remote.supabase

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Supabase backend operations.
 * Handles auth, reading progress sync, library sync, and platform credentials.
 */
@Singleton
class SupabaseRepository @Inject constructor(
    private val appConfig: AppConfig,
    private val clientProvider: SupabaseClientProvider,
) {
    private val tag = "SupabaseRepository"

    private val client get() = clientProvider.client
    val isAvailable: Boolean get() = clientProvider.isAvailable

    // =========================================================================
    // AUTH
    // =========================================================================

    suspend fun signInWithGoogle(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            client?.auth?.signInWith(Google)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Google sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            client?.auth?.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sign-out failed", e)
            Result.failure(e)
        }
    }

    fun currentUser(): UserInfo? = client?.auth?.currentUserOrNull()

    val isAuthenticated: Boolean
        get() = currentUser() != null

    // =========================================================================
    // READING PROGRESS
    // =========================================================================

    @Serializable
    data class ReadingProgressRow(
        val user_id: String? = null,
        val platform: String,
        val story_id: String,
        val story_title: String? = null,
        val last_chapter: Int = 1,
        val last_page: Int = 1,
        val updated_at: String? = null,
    )

    suspend fun saveProgress(platform: String, storyId: String, chapter: Int, page: Int, title: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val row = ReadingProgressRow(
                user_id = user.id,
                platform = platform,
                story_id = storyId,
                story_title = title,
                last_chapter = chapter,
                last_page = page,
            )
            client?.from("reading_progress")?.upsert(row)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Save progress failed", e)
            Result.failure(e)
        }
    }

    suspend fun loadProgress(platform: String, storyId: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val result = client?.from("reading_progress")
                ?.select(Columns.list("last_chapter", "last_page"))
                ?.eq("user_id", user.id)
                ?.eq("platform", platform)
                ?.eq("story_id", storyId)
                ?.decodeSingleOrNull<ReadingProgressRow>()
            if (result != null) {
                Result.success(result.last_chapter to result.last_page)
            } else {
                Result.success(1 to 1)
            }
        } catch (e: Exception) {
            Log.e(tag, "Load progress failed", e)
            Result.failure(e)
        }
    }

    suspend fun getAllProgress(): Result<List<ReadingProgressRow>> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val result = client?.from("reading_progress")
                ?.select()
                ?.eq("user_id", user.id)
                ?.decodeList<ReadingProgressRow>()
            Result.success(result ?: emptyList())
        } catch (e: Exception) {
            Log.e(tag, "Get all progress failed", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // PLATFORM CREDENTIALS
    // =========================================================================

    @Serializable
    data class PlatformCredentialRow(
        val user_id: String? = null,
        val platform: String,
        val encrypted_username: String,
        val encrypted_password: String,
        val platform_user_id: String? = null,
    )

    suspend fun saveCredentials(platform: String, username: String, password: String, platformUserId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val row = PlatformCredentialRow(
                user_id = user.id,
                platform = platform,
                encrypted_username = username, // TODO: encrypt with Android Keystore before storing
                encrypted_password = password,
                platform_user_id = platformUserId,
            )
            client?.from("platform_credentials")?.upsert(row)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Save credentials failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCredentials(platform: String): Result<PlatformCredentialRow?> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val result = client?.from("platform_credentials")
                ?.select()
                ?.eq("user_id", user.id)
                ?.eq("platform", platform)
                ?.decodeSingleOrNull<PlatformCredentialRow>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "Get credentials failed", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // STORY CACHE
    // =========================================================================

    @Serializable
    data class StoryCacheRow(
        val platform: String,
        val story_id: String,
        val metadata: String, // JSON string
    )

    suspend fun cacheStoryMetadata(platform: String, storyId: String, metadataJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val row = StoryCacheRow(platform, storyId, metadataJson)
            client?.from("story_cache")?.upsert(row)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Cache story failed", e)
            Result.failure(e)
        }
    }

    suspend fun getCachedStory(platform: String, storyId: String): Result<StoryCacheRow?> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val result = client?.from("story_cache")
                ?.select()
                ?.eq("platform", platform)
                ?.eq("story_id", storyId)
                ?.decodeSingleOrNull<StoryCacheRow>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "Get cached story failed", e)
            Result.failure(e)
        }
    }
}
