package com.shakeandeat.ui.pick

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.Group
import com.shakeandeat.data.GroupRepository
import com.shakeandeat.data.PlaceHours
import com.shakeandeat.data.PlaceOption
import com.shakeandeat.data.PlacesRepository
import com.shakeandeat.data.toGroupMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HoursState {
    data object Unknown : HoursState
    data object Loading : HoursState
    data class Known(val hours: PlaceHours) : HoursState
}

data class TodaysPickUiState(
    val loaded: Boolean = false,
    val group: Group? = null,
    val winner: PlaceOption? = null,
    val hours: HoursState = HoursState.Unknown,
    val busy: Boolean = false,
    val error: String? = null,
    val reopened: Boolean = false,
)

class TodaysPickViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val groups = GroupRepository()
    private val places = PlacesRepository()
    private val gid: String = savedStateHandle.get<String>("gid").orEmpty()

    private val _state = MutableStateFlow(TodaysPickUiState())
    val state: StateFlow<TodaysPickUiState> = _state.asStateFlow()

    private var hoursFor: String? = null

    init {
        viewModelScope.launch {
            combine(groups.group(gid), groups.options(gid)) { group, options ->
                group to options.firstOrNull { it.id == group?.winnerOptionId }
            }
                .catch { e -> _state.update { it.copy(loaded = true, error = e.toGroupMessage()) } }
                .collect { (group, winner) ->
                    _state.update { it.copy(loaded = true, group = group, winner = winner) }
                    loadHours(winner?.placeId)
                }
        }
    }

    private fun loadHours(placeId: String?) {
        if (placeId == hoursFor) return
        hoursFor = placeId
        if (placeId == null) {
            _state.update { it.copy(hours = HoursState.Unknown) }
            return
        }
        _state.update { it.copy(hours = HoursState.Loading) }
        viewModelScope.launch {
            val result: HoursState = try {
                HoursState.Known(places.hours(placeId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                HoursState.Unknown
            }
            if (hoursFor == placeId) _state.update { it.copy(hours = result) }
        }
    }

    fun shakeAgain() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                groups.reopenVoting(gid)
                _state.update { it.copy(busy = false, reopened = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.toGroupMessage()) }
            }
        }
    }

    fun onReopenedHandled() = _state.update { it.copy(reopened = false) }
}
