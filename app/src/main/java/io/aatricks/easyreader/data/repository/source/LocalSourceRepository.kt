package io.aatricks.easyreader.data.repository.source

import io.aatricks.easyreader.data.local.LibraryDao
import io.aatricks.easyreader.data.model.ContentType
import io.aatricks.easyreader.data.model.ExploreItem
import io.aatricks.easyreader.data.model.LibraryItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NovelSource] wrapper around locally imported content (EPUB/PDF/HTML files added
 * via "Open with" or manual import), backed by the existing library persistence layer.
 *
 * Unlike the web-scraping sources, this doesn't fetch anything remote: "browsing" this
 * source just surfaces whatever the user has already imported into their library, so it
 * requires no auth and doesn't support search-as-a-remote-query (see [PluginRegistry]'s
 * `supportsSearch = false` for the "local" plugin).
 */
@Singleton
class LocalSourceRepository @Inject constructor(
    private val libraryDao: LibraryDao,
) : NovelSource {

    override val name: String = "Local Files"
    override val baseUrl: String = ""
    override val version: String = "1.0"

    override suspend fun getPopularNovels(page: Int, tags: List<String>): List<ExploreItem> {
        if (page > 1) return emptyList()
        return libraryDao.getAllItemsDirect()
            .filter { it.contentType != ContentType.WEB }
            .map(::toExploreItem)
    }

    override suspend fun searchNovels(query: String, page: Int): List<ExploreItem> {
        if (page > 1 || query.isBlank()) return emptyList()
        return libraryDao.getAllItemsDirect()
            .filter { it.contentType != ContentType.WEB && it.title.contains(query, ignoreCase = true) }
            .map(::toExploreItem)
    }

    override suspend fun getNovelDetails(url: String): ExploreItem =
        libraryDao.getItemByUrl(url)?.let(::toExploreItem)
            ?: throw NoSuchElementException("No local item found for $url")

    override suspend fun getTags(): List<String> = emptyList()

    private fun toExploreItem(item: LibraryItem): ExploreItem = ExploreItem(
        title = item.title,
        url = item.url,
        chapterCount = item.totalChapters,
        source = name,
        readingUrl = item.currentChapterUrl.takeIf { it.isNotBlank() },
    )
}
