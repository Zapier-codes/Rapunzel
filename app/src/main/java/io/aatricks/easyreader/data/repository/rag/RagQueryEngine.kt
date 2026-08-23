package io.aatricks.easyreader.data.repository.rag

import android.util.Log
import io.aatricks.easyreader.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val EXCERPT_LENGTH = 300
private const val FALLBACK_PASSAGE_COUNT = 3
private const val FALLBACK_SNIPPET_LENGTH = 200
private const val RETRIEVAL_K = 5

/**
 * Query engine that formats RAG-retrieved context + user question into a prompt
 * and calls the local LLM (llmedge) for generative answers.
 */
@Singleton
class RagQueryEngine @Inject constructor(
    private val appConfig: AppConfig,
    private val retriever: RagRetriever,
) {
    private val tag = "RagQueryEngine"

    data class AiAnswer(
        val answer: String,
        val sources: List<RagRetriever.RetrievedPassage>,
        val confidence: Double,
    )

    /**
     * Ask a question about a book. Retrieves relevant passages, builds a prompt,
     * and returns a generated answer.
     *
     * If llmedge is not available, falls back to extractive answer (concatenate top passages).
     */
    suspend fun ask(question: String, bookId: String? = null): AiAnswer = withContext(Dispatchers.Default) {
        if (!appConfig.isAiRagEnabled) {
            return@withContext AiAnswer(
                answer = "AI assistant is disabled. Enable it in Settings > AI Features.",
                sources = emptyList(),
                confidence = 0.0,
            )
        }

        val passages = retriever.retrieve(question, k = RETRIEVAL_K, bookId = bookId)
        if (passages.isEmpty()) {
            return@withContext AiAnswer(
                answer = "I couldn't find any relevant passages to answer your question. " +
                    "Try rephrasing or make sure the book is indexed.",
                sources = emptyList(),
                confidence = 0.0,
            )
        }

        val context = passages.joinToString("\n\n") { "[Excerpt] ${it.snippet.take(EXCERPT_LENGTH)}..." }
        val prompt = buildPrompt(context, question)

        // Try generative LLM first
        val generativeAnswer = tryGenerativeLLM(prompt)
        if (generativeAnswer != null) {
            return@withContext AiAnswer(
                answer = generativeAnswer,
                sources = passages,
                confidence = passages.firstOrNull()?.score ?: 0.0,
            )
        }

        // Fallback: extractive summary
        val fallback = passages.take(FALLBACK_PASSAGE_COUNT).joinToString("\n\n") {
            "• ${it.snippet.take(FALLBACK_SNIPPET_LENGTH)}..."
        }

        AiAnswer(
            answer = "Based on the text:\n\n$fallback",
            sources = passages,
            confidence = passages.firstOrNull()?.score ?: 0.0,
        )
    }

    private fun buildPrompt(context: String, question: String): String {
        return """You are a helpful reading assistant. Use ONLY the provided excerpts from
the book to answer the user's question. If the answer is not in the excerpts, say so honestly.

EXCERPTS:
$context

QUESTION: $question

ANSWER:""".trimIndent()
    }

    private suspend fun tryGenerativeLLM(prompt: String): String? {
        return try {
            // llmedge integration — call via reflection to avoid hard dependency on AI flavor
            val llmClass = Class.forName("com.aatricks.llmedge.LLMEdge")
            val instance = llmClass.getMethod("getInstance").invoke(null)
            val result = instance.javaClass.getMethod("generate", String::class.java)
                .invoke(instance, prompt) as? String
            result
        } catch (e: RuntimeException) {
            Log.d(tag, "Generative LLM not available: ${e.message}")
            null
        }
    }
}
