package io.aatricks.easyreader.ui.screens.aichat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.aatricks.easyreader.data.repository.rag.RagQueryEngine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val queryEngine: RagQueryEngine,
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    val isLoading = mutableStateOf(false)

    fun sendMessage(text: String, bookId: String? = null) {
        messages.add(ChatMessage(text = text, isUser = true))
        isLoading.value = true

        viewModelScope.launch {
            try {
                val answer = queryEngine.ask(text, bookId)
                messages.add(ChatMessage(text = answer.answer, isUser = false))
            } catch (e: Exception) {
                messages.add(ChatMessage(text = "Sorry, something went wrong: ${e.message}", isUser = false))
            } finally {
                isLoading.value = false
            }
        }
    }
}
