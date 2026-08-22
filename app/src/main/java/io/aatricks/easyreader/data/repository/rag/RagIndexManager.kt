package io.aatricks.easyreader.data.repository.rag

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import io.aatricks.easyreader.data.local.AppDatabase
import io.aatricks.easyreader.data.local.ChapterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Builds and maintains an inverted TF-IDF index over book chapters.
 * Fully local — no network calls, no model weights.
 */
@Singleton
class RagIndexManager @Inject constructor(
    private val appConfig: AppConfig,
    private val database: AppDatabase,
) {
    private val tag = "RagIndexManager"

    // In-memory index: token -> list of (chapterId, tf-idf score)
    val invertedIndex = ConcurrentHashMap<String, MutableList<Posting>>()
    // Document frequency: token -> number of chapters containing it
    private val docFreq = ConcurrentHashMap<String, Int>()
    // Total chapters indexed
    private var totalDocs = 0

    data class Posting(
        val chapterId: Long,
        val bookId: String,
        val score: Double,
        val snippet: String,
    )

    /** Build index for a specific book. Call after import or when RAG is first enabled. */
    suspend fun indexBook(bookId: String) = withContext(Dispatchers.Default) {
        if (!appConfig.isAiRagEnabled) {
            Log.d(tag, "RAG disabled by feature flag")
            return@withContext
        }
        try {
            val chapters = database.chapterDao().getChaptersForBook(bookId)
            chapters.forEach { chapter ->
                indexChapter(chapter, bookId)
            }
            totalDocs += chapters.size
            Log.i(tag, "Indexed ${chapters.size} chapters for book $bookId")
        } catch (e: Exception) {
            Log.e(tag, "Indexing failed for book $bookId", e)
        }
    }

    /** Remove a book from the index. */
    suspend fun removeBook(bookId: String) = withContext(Dispatchers.Default) {
        invertedIndex.values.forEach { postings ->
            postings.removeAll { it.bookId == bookId }
        }
        Log.i(tag, "Removed book $bookId from index")
    }

    /** Clear entire index. */
    fun clearIndex() {
        invertedIndex.clear()
        docFreq.clear()
        totalDocs = 0
        Log.i(tag, "Index cleared")
    }

    private fun indexChapter(chapter: ChapterEntity, bookId: String) {
        val tokens = tokenize(chapter.content ?: return)
        val tf = tokens.groupingBy { it }.eachCount()
        val maxFreq = tf.values.maxOrNull()?.toDouble() ?: 1.0

        tf.forEach { (token, count) ->
            val normalizedTf = count / maxFreq
            val postings = invertedIndex.getOrPut(token) { mutableListOf() }
            postings.add(Posting(
                chapterId = chapter.id,
                bookId = bookId,
                score = normalizedTf,
                snippet = chapter.content!!.take(200)
            ))
            docFreq.merge(token, 1, Int::plus)
        }
    }

    /** Tokenize text: lowercase, remove punctuation, split on whitespace. */
    fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\s]"), " ")
            .split(Regex("\s+"))
            .filter { it.length > 2 }
    }

    /** Compute TF-IDF score for a token in a specific document. */
    fun tfidf(token: String, chapterId: Long): Double {
        val postings = invertedIndex[token] ?: return 0.0
        val posting = postings.find { it.chapterId == chapterId } ?: return 0.0
        val df = docFreq[token] ?: 1
        val idf = ln((totalDocs + 1.0) / (df + 1.0)) + 1.0
        return posting.score * idf
    }

    /** Get postings for a token. */
    fun getPostings(token: String): List<Posting> =
        invertedIndex[token] ?: emptyList()

    /** Check if index has data. */
    fun isIndexed(): Boolean = totalDocs > 0
}
