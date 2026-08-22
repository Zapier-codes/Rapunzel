package io.aatricks.easyreader.data.repository.source

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.local.PreferencesManager
import io.aatricks.easyreader.data.model.ExploreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject

/**
 * Royal Road HTML scraper extending BaseJsoupSource.
 * No official API exists — all data comes from HTML parsing.
 *
 * Selector strategy based on current site structure (2026).
 * Kill-switch ready: if structure changes, disable via FEATURE_ROYALROAD.
 */
class RoyalRoadSource @Inject constructor(
    preferencesManager: PreferencesManager,
    okHttpClient: okhttp3.OkHttpClient,
    private val appConfig: AppConfig,
) : BaseJsoupSource(preferencesManager, okHttpClient) {

    override val name: String = "Royal Road"
    override val baseUrl: String = appConfig.royalRoadBaseUrl
    override val version: String = "1.0.0"

    private val tag = "RoyalRoadSource"

    val isEnabled: Boolean
        get() = appConfig.isRoyalRoadEnabled

    // =========================================================================
    // BROWSE
    // =========================================================================

    override suspend fun getPopularNovels(page: Int, tags: List<String>): List<ExploreItem> = io {
        if (!isEnabled) return@io emptyList()
        try {
            val url = "$baseUrl/fictions/best-ranked?page=$page"
            val doc = getDocument(url)
            doc.select(".fiction-list-item").mapNotNull { it.toExploreItem() }
        } catch (e: RuntimeException) {
            Log.e(tag, "Popular fetch failed", e)
            emptyList()
        }
    }

    override suspend fun getNovels(mode: BrowseMode, page: Int, tags: List<String>): List<ExploreItem> = io {
        if (!isEnabled) return@io emptyList()
        try {
            val url = when (mode) {
                BrowseMode.POPULAR -> "$baseUrl/fictions/best-ranked?page=$page"
                BrowseMode.LATEST -> "$baseUrl/fictions/latest-updates?page=$page"
                BrowseMode.NEW -> "$baseUrl/fictions/new-releases?page=$page"
            }
            val doc = getDocument(url)
            doc.select(".fiction-list-item").mapNotNull { it.toExploreItem() }
        } catch (e: RuntimeException) {
            Log.e(tag, "Browse fetch failed", e)
            emptyList()
        }
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    override suspend fun searchNovels(query: String, page: Int): List<ExploreItem> = io {
        if (!isEnabled) return@io emptyList()
        try {
            val url = "$baseUrl/fictions/search?search=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page"
            val doc = getDocument(url)
            doc.select(".fiction-list-item").mapNotNull { it.toExploreItem() }
        } catch (e: RuntimeException) {
            Log.e(tag, "Search failed", e)
            emptyList()
        }
    }

    // =========================================================================
    // DETAIL
    // =========================================================================

    override suspend fun getNovelDetails(url: String): ExploreItem = io {
        if (!isEnabled) error("Royal Road disabled")
        val doc = getDocument(url)
        val fictionId = url.substringAfter("/fiction/").takeWhile { it.isDigit() }

        ExploreItem(
            id = fictionId,
            title = doc.selectFirst("h1")?.text()?.trim() ?: "Unknown",
            author = doc.selectFirst(".fic-header .author")?.text()?.trim()
                ?: doc.selectFirst("[property="author"]")?.text()?.trim()
                ?: "Unknown",
            coverUrl = doc.selectFirst(".fic-header img")?.findImage()
                ?: doc.selectFirst(".cover-art-container img")?.findImage()
                ?: "",
            description = doc.selectFirst(".description")?.text()?.trim()
                ?: doc.selectFirst("[property="description"]")?.attr("content")
                ?: "",
            url = url,
            status = if (doc.selectFirst(".fa-circle-check") != null) "Completed" else "Ongoing",
            genres = doc.select(".tags .tag").map { it.text().trim() },
            rating = doc.selectFirst(".star")?.attr("title")?.toDoubleOrNull(),
            totalChapters = doc.select("table#chapters tbody tr").size,
            sourceName = name,
        )
    }

    // =========================================================================
    // CHAPTERS
    // =========================================================================

    suspend fun getChapterList(fictionUrl: String): List<RoyalRoadChapter> = io {
        if (!isEnabled) return@io emptyList()
        val doc = getDocument(fictionUrl)
        doc.select("table#chapters tbody tr").mapIndexed { index, row ->
            val link = row.selectFirst("a[href]")
            RoyalRoadChapter(
                id = link?.attr("href")?.substringAfterLast("/") ?: "",
                title = link?.text()?.trim() ?: "Chapter ${index + 1}",
                url = link?.absoluteUrl("href") ?: "",
                chapterNumber = index + 1,
                releaseDate = row.selectFirst(".text-right")?.text()?.trim(),
            )
        }
    }

    suspend fun getChapterContent(chapterUrl: String): String = io {
        if (!isEnabled) return@io ""
        val doc = getDocument(chapterUrl)
        doc.selectFirst(".chapter-content")?.html()
            ?: doc.selectFirst("[property="articleBody"]")?.html()
            ?: ""
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun Element.toExploreItem(): ExploreItem? {
        val titleEl = selectFirst("h2 a, .fiction-title a") ?: return null
        val title = titleEl.text().trim()
        val url = titleEl.absoluteUrl("href")
        val fictionId = url.substringAfter("/fiction/").takeWhile { it.isDigit() }

        return ExploreItem(
            id = fictionId,
            title = title,
            author = selectFirst(".author")?.text()?.trim() ?: "Unknown",
            coverUrl = selectFirst("img")?.findImage() ?: "",
            description = selectFirst(".fiction-description")?.text()?.trim() ?: "",
            url = url,
            status = "Ongoing",
            genres = select(".tags .tag").map { it.text().trim() },
            rating = selectFirst(".star")?.attr("title")?.toDoubleOrNull(),
            totalChapters = null,
            sourceName = name,
        )
    }

    data class RoyalRoadChapter(
        val id: String,
        val title: String,
        val url: String,
        val chapterNumber: Int,
        val releaseDate: String? = null,
    )
}
