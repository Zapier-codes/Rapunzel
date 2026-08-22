package io.aatricks.easyreader.data.repository.rag

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Retrieves top-k relevant passages from the RAG index for a given query.
 * Uses cosine similarity over TF-IDF vectors.
 */
@Singleton
class RagRetriever @Inject constructor(
    private val appConfig: AppConfig,
    private val indexManager: RagIndexManager,
) {
    private val tag = "RagRetriever"

    data class RetrievedPassage(
        val chapterId: Long,
        val bookId: String,
        val snippet: String,
        val score: Double,
    )

    /**
     * Retrieve top-k most relevant passages for the query.
     * @param query User question
     * @param k Number of passages to return (default 5)
     * @param bookId Optional: restrict search to a single book
     */
    suspend fun retrieve(query: String, k: Int = 5, bookId: String? = null): List<RetrievedPassage> = withContext(Dispatchers.Default) {
        if (!appConfig.isAiRagEnabled) {
            Log.d(tag, "RAG disabled")
            return@withContext emptyList()
        }
        if (!indexManager.isIndexed()) {
            Log.w(tag, "Index is empty — no passages to retrieve")
            return@withContext emptyList()
        }

        val queryTokens = indexManager.tokenize(query)
        if (queryTokens.isEmpty()) return@withContext emptyList()

        // Score all documents using cosine similarity
        val scores = mutableMapOf<Long, Double>()
        val snippets = mutableMapOf<Long, String>()
        val bookIds = mutableMapOf<Long, String>()

        queryTokens.forEach { token ->
            val postings = indexManager.getPostings(token)
            postings.forEach { posting ->
                if (bookId != null && posting.bookId != bookId) return@forEach
                val tfidf = indexManager.tfidf(token, posting.chapterId)
                scores.merge(posting.chapterId, tfidf, Double::plus)
                snippets[posting.chapterId] = posting.snippet
                bookIds[posting.chapterId] = posting.bookId
            }
        }

        // Normalize by query vector magnitude
        val queryMagnitude = sqrt(queryTokens.size.toDouble())
        val normalizedScores = scores.mapValues { (_, score) ->
            score / queryMagnitude
        }

        // Return top-k
        normalizedScores.entries
            .sortedByDescending { it.value }
            .take(k)
            .map { (chapterId, score) ->
                RetrievedPassage(
                    chapterId = chapterId,
                    bookId = bookIds[chapterId] ?: "",
                    snippet = snippets[chapterId] ?: "",
                    score = score,
                )
            }
    }
}
