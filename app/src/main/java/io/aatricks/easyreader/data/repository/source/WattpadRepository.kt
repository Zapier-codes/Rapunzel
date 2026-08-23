package io.aatricks.easyreader.data.repository.source

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.model.SourceBook
import io.aatricks.easyreader.data.model.SourceChapter
import io.aatricks.easyreader.data.remote.wattpad.WattpadApiService
import io.aatricks.easyreader.data.remote.wattpad.WattpadAuthInterceptor
import io.aatricks.easyreader.data.remote.wattpad.WattpadAuthRequest
import io.aatricks.easyreader.data.remote.wattpad.WattpadStory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val WATTPAD_PAGE_SIZE = 20
private const val CHUNK_SIZE = 3000

/**
 * Repository for Wattpad source integration.
 * Wraps the undocumented v3/apiv2 REST API with caching and error handling.
 */
@Singleton
class WattpadRepository @Inject constructor(
    private val appConfig: AppConfig,
    private val apiService: WattpadApiService,
    private val authInterceptor: WattpadAuthInterceptor,
) : NovelSource {

    private val tag = "WattpadRepository"

    override val id: String = "wattpad"
    override val name: String = "Wattpad"
    override val language: String = "en"
    override val supportsLatest: Boolean = true
    override val supportsSearch: Boolean = true
    override val requiresAuth: Boolean = false // Search works without auth; reading may require it

    val isEnabled: Boolean
        get() = appConfig.isWattpadEnabled

    // =========================================================================
    // AUTH
    // =========================================================================

    suspend fun authenticate(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(WattpadAuthRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                body?.accessToken?.let { token ->
                    authInterceptor.setToken(token)
                    Result.success(token)
                } ?: Result.failure(Exception("Empty auth response"))
            } else {
                Result.failure(Exception("Auth failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: RuntimeException) {
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

    override suspend fun search(query: String, page: Int): List<SourceBook> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        try {
            val response = apiService.searchStories(
                query = query,
                limit = WATTPAD_PAGE_SIZE,
                offset = (page - 1) * WATTPAD_PAGE_SIZE
            )
            if (response.isSuccessful) {
                response.body()?.stories?.map { it.toSourceBook() } ?: emptyList()
            } else {
                Log.w(tag, "Search failed: ${response.code()}")
                emptyList()
            }
        } catch (e: RuntimeException) {
            Log.e(tag, "Search error", e)
            emptyList()
        }
    }

    // =========================================================================
    // STORY DETAIL
    // =========================================================================

    override suspend fun getMangaDetails(mangaUrl: String): SourceBook = withContext(Dispatchers.IO) {
        if (!isEnabled) error("Wattpad source disabled")
        val storyId = extractStoryId(mangaUrl)
        val response = apiService.getStory(storyId)
        if (response.isSuccessful) {
            response.body()?.toSourceBook() ?: throw RuntimeException("Empty story response")
        } else {
            throw RuntimeException("Failed to load story: ${response.code()}")
        }
    }

    override suspend fun getChapterList(mangaUrl: String): List<SourceChapter> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        val storyId = extractStoryId(mangaUrl)
        val response = apiService.getStory(storyId)
        if (response.isSuccessful) {
            response.body()?.parts?.mapIndexed { index, part ->
                SourceChapter(
                    id = part.id.toString(),
                    url = part.url ?: "https://www.wattpad.com/v4/parts/${part.id}/text",
                    name = part.title ?: "Part ${index + 1}",
                    uploadDate = part.createDate,
                    chapterNumber = index + 1,
                )
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // =========================================================================
    // CHAPTER CONTENT
    // =========================================================================

    override suspend fun getPageList(chapter: SourceChapter): List<String> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        try {
            val partId = extractPartId(chapter.url)
            val response = apiService.getPartText(partId)
            if (response.isSuccessful) {
                val text = response.body()?.text
                if (text != null) {
                    // Split long text into pages (~3000 chars each)
                    listOf(text.chunked(CHUNK_SIZE).joinToString("\n\n---\n\n"))
                } else emptyList()
            } else {
                Log.w(tag, "Chapter text failed: ${response.code()}")
                emptyList()
            }
        } catch (e: RuntimeException) {
            Log.e(tag, "Chapter load error", e)
            emptyList()
        }
    }

    // =========================================================================
    // LATEST (popular stories)
    // =========================================================================

    override suspend fun getLatestUpdates(page: Int): List<SourceBook> = withContext(Dispatchers.IO) {
        if (!isEnabled) return@withContext emptyList()
        // Wattpad doesn't have a "latest" endpoint; use search with empty query as fallback
        search("", page)
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun extractStoryId(url: String): Long {
        // URLs like: https://www.wattpad.com/story/12345678-title
        return url.substringAfter("/story/", "")
            .takeWhile { it.isDigit() }
            .toLongOrNull()
            ?: throw IllegalArgumentException("Invalid Wattpad story URL: $url")
    }

    private fun extractPartId(url: String): Long {
        // URLs like: https://www.wattpad.com/v4/parts/12345678/text
        return url.substringAfter("/parts/", "")
            .takeWhile { it.isDigit() }
            .toLongOrNull()
            ?: throw IllegalArgumentException("Invalid Wattpad part URL: $url")
    }

    private fun WattpadStory.toSourceBook(): SourceBook {
        return SourceBook(
            id = id.toString(),
            title = title,
            url = "https://www.wattpad.com/story/$id",
            coverUrl = cover,
            author = author?.name ?: author?.username ?: "Unknown",
            description = description ?: "",
            status = if (completed == true) "Completed" else "Ongoing",
            genre = tags?.firstOrNull() ?: "",
            source = id,
            totalChapters = numParts,
        )
    }
}
