package com.shakeandeat.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.ChatMessage
import com.shakeandeat.data.ChatRepository
import com.shakeandeat.data.GroupRepository
import com.shakeandeat.data.toGroupMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val myUid: String? = null,
    val groupName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val loaded: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val chat = ChatRepository()
    private val groups = GroupRepository()
    private val gid: String = savedStateHandle.get<String>("gid").orEmpty()

    val draft: StateFlow<String> = savedStateHandle.getStateFlow(DRAFT, "")

    private val _state = MutableStateFlow(ChatUiState(myUid = chat.currentUid))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            groups.group(gid)
                .catch { }
                .collect { group -> _state.update { it.copy(groupName = group?.name.orEmpty()) } }
        }
        viewModelScope.launch {
            chat.messagesFor(gid)
                .catch { e -> _state.update { it.copy(loaded = true, error = e.toGroupMessage()) } }
                .collect { list -> _state.update { it.copy(messages = list, loaded = true) } }
        }
    }

    fun onDraftChange(text: String) {
        savedStateHandle[DRAFT] = text.take(ChatRepository.MAX_LENGTH)
        if (_state.value.error != null) _state.update { it.copy(error = null) }
    }

    fun send() {
        val text = draft.value.trim()
        if (text.isEmpty() || _state.value.sending) return
        savedStateHandle[DRAFT] = ""
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                chat.send(gid, text)
                _state.update { it.copy(sending = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                savedStateHandle[DRAFT] = text
                _state.update { it.copy(sending = false, error = "Message didn't send. Try again.") }
            }
        }
    }

    private companion object {
        const val DRAFT = "draft"
    }
}
