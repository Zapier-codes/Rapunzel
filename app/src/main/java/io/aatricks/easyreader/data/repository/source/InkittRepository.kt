package io.aatricks.easyreader.data.repository.source

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.model.SourceBook
import io.aatricks.easyreader.data.model.SourceChapter
import io.aatricks.easyreader.data.remote.inkitt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inkitt source integration via reverse-engineered mobile API.
 *
 * Endpoints are based on observed traffic from the Inkitt Android app
 * and the ik-mini Rust crate documentation. Actual paths may require
 * adjustment after live traffic inspection.
 */
@Singleton
class InkittRepository @Inject constructor(
    private val appConfig: AppConfig,
    private val apiService: InkittApiService,
    private val authInterceptor: InkittAuthInterceptor,
) : NovelSource {

    private val tag = "InkittRepository"

    override val id: String = "inkitt"
    override val name: String = "Inkitt"
    override val language: String = "en"
    override val supportsLatest: Boolean = true
    override val supportsSearch: Boolean = true
    override val requiresAuth: Boolean = true

    val isEnabled: Boolean
        get() = appConfig.isInkittEnabled

    // =========================================================================
    // AUTH
    // =========================================================================

    suspend fun authenticate(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(InkittAuthRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                body?.accessToken?.let { token ->
                    authInterceptor.setToken(token)
                    Result.success(token)
                } ?: Result.failure(Exception("Empty auth response"))
            } else {
                Result.failure(Exception("Auth failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Auth error", e)
            Result.failure(e)
        }
    }

    fun logout() {
        authInterceptor.clearToken()
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    override suspend fun searchNovels(query: String, page: Int): List<ExploreItem> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        try {
            val response = apiService.search(query, page)
            if (response.isSuccessful) {
                response.body()?.stories?.map { it.toExploreItem() } ?: emptyList()
            } else {
                Log.w(tag, "Search failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Search error", e)
            emptyList()
        }
    }

    // =========================================================================
    // BROWSE
    // =========================================================================

    override suspend fun getPopularNovels(page: Int, tags: List<String>): List<ExploreItem> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        try {
            val response = apiService.getPopular(page)
            if (response.isSuccessful) {
                response.body()?.stories?.map { it.toExploreItem() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Popular fetch error", e)
            emptyList()
        }
    }

    override suspend fun getNovels(mode: BrowseMode, page: Int, tags: List<String>): List<ExploreItem> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        try {
            val response = when (mode) {
                BrowseMode.POPULAR -> apiService.getPopular(page)
                BrowseMode.LATEST -> apiService.getLatest(page)
                BrowseMode.NEW -> apiService.getLatest(page) // Inkitt has no "new" endpoint
            }
            if (response.isSuccessful) {
                response.body()?.stories?.map { it.toExploreItem() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Browse error", e)
            emptyList()
        }
    }

    // =========================================================================
    // DETAIL
    // =========================================================================

    override suspend fun getNovelDetails(url: String): ExploreItem = withContext(Dispatchers.IO) {
        if (!isEnabled) throw IllegalStateException("Inkitt disabled")
        val storyId = extractStoryId(url)
        val response = apiService.getStory(storyId)
        if (response.isSuccessful) {
            response.body()?.toExploreItem() ?: throw Exception("Empty story response")
        } else {
            throw Exception("Failed to load story: ${response.code()}")
        }
    }

    suspend fun getChapterList(storyUrl: String): List<SourceChapter> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        val storyId = extractStoryId(storyUrl)
        val response = apiService.getChapters(storyId)
        if (response.isSuccessful) {
            response.body()?.mapIndexed { index, ch ->
                SourceChapter(
                    id = ch.id.toString(),
                    url = ch.url ?: "https://www.inkitt.com/api/v1/chapters/${ch.id}",
                    name = ch.title ?: "Chapter ${index + 1}",
                    uploadDate = ch.createdAt,
                    chapterNumber = ch.number,
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // =========================================================================
    // CHAPTER CONTENT
    // =========================================================================

    suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext ""
        val chapterId = extractChapterId(chapterUrl)
        val response = apiService.getChapter(chapterId)
        if (response.isSuccessful) {
            response.body()?.content ?: ""
        } else {
            Log.w(tag, "Chapter load failed: ${response.code()}")
            ""
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun extractStoryId(url: String): Long {
        return url.substringAfter("/story/", "")
            .takeWhile { it.isDigit() }
            .toLongOrNull()
            ?: throw IllegalArgumentException("Invalid Inkitt story URL: $url")
    }

    private fun extractChapterId(url: String): Long {
        return url.substringAfter("/chapters/", "")
            .takeWhile { it.isDigit() }
            .toLongOrNull()
            ?: throw IllegalArgumentException("Invalid Inkitt chapter URL: $url")
    }

    private fun InkittStory.toExploreItem(): ExploreItem {
        return ExploreItem(
            id = id.toString(),
            title = title,
            url = "https://www.inkitt.com/story/$id",
            coverUrl = cover,
            author = author?.displayName ?: author?.username ?: "Unknown",
            description = description ?: "",
            status = if (completed) "Completed" else "Ongoing",
            genres = tags ?: emptyList(),
            rating = null,
            totalChapters = chapterCount,
            sourceName = name,
        )
    }
}
