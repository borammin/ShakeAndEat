package com.shakeandeat.ui.places

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.LocationProvider
import com.shakeandeat.data.NearbyPlace
import com.shakeandeat.data.PlacesRepository
import com.shakeandeat.data.SearchResult
import com.shakeandeat.data.toPlacesMessage
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface NearbyState {
    data object Idle : NearbyState
    data object Loading : NearbyState
    data class Loaded(val places: List<NearbyPlace>) : NearbyState
    data class Error(val message: String) : NearbyState
    data object Denied : NearbyState
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val places: List<SearchResult>) : SearchState
    data class Error(val message: String) : SearchState
}

class NearbyPlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val places = PlacesRepository()
    private val location = LocationProvider(application)

    private val _state = MutableStateFlow<NearbyState>(NearbyState.Idle)
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search: StateFlow<SearchState> = _search.asStateFlow()

    // One token per typing session, so Google bills the whole search as a single session.
    private var sessionToken = UUID.randomUUID().toString()
    private var searchJob: Job? = null

    /** Type-ahead: waits for 3+ characters and a short pause, so we don't search on every key. */
    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()
        val clean = text.trim()
        if (clean.length < MIN_QUERY_LENGTH) {
            _search.value = SearchState.Idle
            return
        }
        _search.value = SearchState.Loading
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _search.value = try {
                SearchState.Results(places.search(clean, sessionToken).take(MAX_RESULTS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchState.Error(e.toPlacesMessage())
            }
        }
    }

    /** After a pick the search is over: clear it and start a new billing session. */
    fun searchPicked() {
        searchJob?.cancel()
        _query.value = ""
        _search.value = SearchState.Idle
        sessionToken = UUID.randomUUID().toString()
    }

    fun load(exclude: Set<String>) {
        _state.value = NearbyState.Loading
        viewModelScope.launch {
            val next: NearbyState = try {
                val here = location.current()
                if (here == null) {
                    NearbyState.Error("Couldn't get your location. Turn on location and try again.")
                } else {
                    NearbyState.Loaded(
                        places.nearby(here.latitude, here.longitude).filterNot { it.placeId in exclude },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NearbyState.Error(e.toPlacesMessage())
            }
            _state.update { if (it == NearbyState.Loading) next else it }
        }
    }

    fun denied() {
        _state.value = NearbyState.Denied
    }

    fun added(place: NearbyPlace) = _state.update {
        if (it is NearbyState.Loaded) it.copy(places = it.places - place) else it
    }

    fun reset() {
        _state.value = NearbyState.Idle
        searchPicked()
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
        const val DEBOUNCE_MS = 300L
        const val MAX_RESULTS = 5
    }
}
