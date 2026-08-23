package io.aatricks.easyreader.data.remote.supabase

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Supabase backend operations.
 * Handles auth, reading progress sync, library sync, and platform credentials.
 */
@Singleton
class SupabaseRepository @Inject constructor(
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
        } catch (e: RuntimeException) {
            Log.e(tag, "Google sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            client?.auth?.signOut()
            Result.success(Unit)
        } catch (e: RuntimeException) {
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
        @SerialName("user_id") val userId: String? = null,
        val platform: String,
        @SerialName("story_id") val storyId: String,
        @SerialName("story_title") val storyTitle: String? = null,
        @SerialName("last_chapter") val lastChapter: Int = 1,
        @SerialName("last_page") val lastPage: Int = 1,
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    suspend fun saveProgress(platform: String, storyId: String, chapter: Int, page: Int, title: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val row = ReadingProgressRow(
                userId = user.id,
                platform = platform,
                storyId = storyId,
                storyTitle = title,
                lastChapter = chapter,
                lastPage = page,
            )
            client?.from("reading_progress")?.upsert(row)
            Result.success(Unit)
        } catch (e: RuntimeException) {
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
                Result.success(result.lastChapter to result.lastPage)
            } else {
                Result.success(1 to 1)
            }
        } catch (e: RuntimeException) {
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
        } catch (e: RuntimeException) {
            Log.e(tag, "Get all progress failed", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // PLATFORM CREDENTIALS
    // =========================================================================

    @Serializable
    data class PlatformCredentialRow(
        @SerialName("user_id") val userId: String? = null,
        val platform: String,
        @SerialName("encrypted_username") val encryptedUsername: String,
        @SerialName("encrypted_password") val encryptedPassword: String,
        @SerialName("platform_user_id") val platformUserId: String? = null,
    )

    suspend fun saveCredentials(platform: String, username: String, password: String, platformUserId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        val user = currentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val row = PlatformCredentialRow(
                userId = user.id,
                platform = platform,
                encryptedUsername = username, // NOTE: encrypt with Android Keystore before storing
                encryptedPassword = password,
                platformUserId = platformUserId,
            )
            client?.from("platform_credentials")?.upsert(row)
            Result.success(Unit)
        } catch (e: RuntimeException) {
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
        } catch (e: RuntimeException) {
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
        @SerialName("story_id") val storyId: String,
        val metadata: String, // JSON string
    )

    suspend fun cacheStoryMetadata(platform: String, storyId: String, metadataJson: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext Result.failure(Exception("Supabase not configured"))
        try {
            val row = StoryCacheRow(platform, storyId, metadataJson)
            client?.from("story_cache")?.upsert(row)
            Result.success(Unit)
        } catch (e: RuntimeException) {
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
        } catch (e: RuntimeException) {
            Log.e(tag, "Get cached story failed", e)
            Result.failure(e)
        }
    }
}
