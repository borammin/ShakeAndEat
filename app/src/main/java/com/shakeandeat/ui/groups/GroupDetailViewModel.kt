package com.shakeandeat.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.ChatRepository
import com.shakeandeat.data.Group
import com.shakeandeat.data.GroupRepository
import com.shakeandeat.data.NearbyPlace
import com.shakeandeat.data.SearchResult
import com.shakeandeat.data.PlaceOption
import com.shakeandeat.data.WinnerLogic
import com.shakeandeat.data.toGroupMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailUiState(
    val myUid: String? = null,
    val group: Group? = null,
    val options: List<PlaceOption> = emptyList(),
    val loaded: Boolean = false,
    val error: String? = null,
    val busy: Boolean = false,
    val openPick: Boolean = false,
) {
    val winner: PlaceOption?
        get() = group?.winnerOptionId?.let { id -> options.firstOrNull { it.id == id } }
}

/** One group; State is held here so it survives rotation. */
class GroupDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = GroupRepository()
    private val chat = ChatRepository()
    private val gid: String = savedStateHandle.get<String>("gid").orEmpty()

    private val _state = MutableStateFlow(GroupDetailUiState(myUid = repository.currentUid))
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.group(gid)
                .catch { e -> _state.update { it.copy(loaded = true, error = e.toGroupMessage()) } }
                .collect { group -> _state.update { it.copy(group = group, loaded = true) } }
        }
        viewModelScope.launch {
            repository.options(gid)
                .catch { e -> _state.update { it.copy(error = e.toGroupMessage()) } }
                .collect { list -> _state.update { it.copy(options = list) } }
        }
    }

    fun addOption(name: String) {
        val me = _state.value.myUid ?: return
        val clean = name.trim()
        when {
            clean.isEmpty() -> return fail("Type the name of a place.")
            clean.length > 80 -> return fail("Place names can be up to 80 characters.")
        }
        write { repository.addOption(gid, clean, me) }
    }

    fun addPlace(place: NearbyPlace) {
        val me = _state.value.myUid ?: return
        write { repository.addOption(gid, place.name, me, place.placeId, place.distanceMi) }
    }

    fun addSearchedPlace(place: SearchResult) {
        val me = _state.value.myUid ?: return
        write { repository.addOption(gid, place.name, me, place.placeId, null) }
    }

    fun toggleVote(option: PlaceOption) {
        val me = _state.value.myUid ?: return
        if (_state.value.group?.isDecided == true) return
        val voted = me !in option.voteUids
        write(showBusy = false) { repository.setVote(gid, option.id, me, voted) }
    }

    /** Picks the winner with [WinnerLogic] and saves it for everyone in the group. */
    fun pickWinner() {
        val options = _state.value.options
        if (options.size < 2) return fail("Add at least 2 options")
        val winner = WinnerLogic.pick(options) ?: return fail("Add at least 2 options")
        write {
            repository.decide(gid, winner.id)
            try {
                chat.postPick(gid, winner.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
            _state.update { it.copy(openPick = true) }
        }
    }

    fun onPickOpened() = _state.update { it.copy(openPick = false) }

    fun reopenVoting() = write { repository.reopenVoting(gid) }

    fun clearError() = _state.update { if (it.error == null) it else it.copy(error = null) }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

    private fun write(showBusy: Boolean = true, block: suspend () -> Unit) {
        if (showBusy && _state.value.busy) return
        _state.update { it.copy(busy = showBusy, error = null) }
        viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.toGroupMessage()) }
            }
        }
    }
}
