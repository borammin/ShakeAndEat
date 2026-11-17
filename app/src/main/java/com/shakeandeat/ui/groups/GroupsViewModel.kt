package com.shakeandeat.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.Group
import com.shakeandeat.data.GroupCodes
import com.shakeandeat.data.GroupRepository
import com.shakeandeat.data.isPermissionDenied
import com.shakeandeat.data.toGroupMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    /** False until Firestore has answered, so we never show "no groups" before we know. */
    val loaded: Boolean = false,
    val loadError: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    /** Set after creating or joining; the screen opens that group once and clears it. */
    val openGroup: String? = null,
)

class GroupsViewModel(
    private val repository: GroupRepository = GroupRepository(),
) : ViewModel() {

    private val uid: String? = repository.currentUid
    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    init {
        val me = uid
        if (me != null) {
            viewModelScope.launch {
                repository.myGroups(me)
                    .catch { e -> _state.update { it.copy(loaded = true, loadError = e.toGroupMessage()) } }
                    .collect { list ->
                        _state.update { it.copy(groups = list, loaded = true, loadError = null) }
                    }
            }
        }
    }

    fun createGroup(name: String) {
        val me = uid ?: return
        val clean = name.trim()
        val problem = when {
            clean.isEmpty() -> "Give your group a name."
            clean.length > 40 -> "Group names can be up to 40 characters."
            else -> null
        }
        if (problem != null) return fail(problem)
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val code = repository.createGroup(clean, me)
                _state.update { it.copy(busy = false, openGroup = code) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.toGroupMessage()) }
            }
        }
    }

    fun joinGroup(rawCode: String) {
        val me = uid ?: return
        val code = GroupCodes.normalize(rawCode)
        if (!GroupCodes.isValid(code)) return fail("Invite codes are ${GroupCodes.LENGTH} letters or numbers.")
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.joinGroup(code, me)
                _state.update { it.copy(busy = false, openGroup = code) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = if (e.isPermissionDenied()) {
                    "Couldn't join. Check the code, or the group may be full (20 members)."
                } else {
                    e.toGroupMessage()
                }
                _state.update { it.copy(busy = false, error = message) }
            }
        }
    }

    fun clearError() = _state.update { if (it.error == null) it else it.copy(error = null) }

    fun onOpened() = _state.update { it.copy(openGroup = null) }

    private fun fail(message: String) = _state.update { it.copy(error = message) }
}
