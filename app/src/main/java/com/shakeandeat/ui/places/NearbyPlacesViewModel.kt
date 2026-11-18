package com.shakeandeat.ui.places

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.LocationProvider
import com.shakeandeat.data.NearbyPlace
import com.shakeandeat.data.PlacesRepository
import kotlinx.coroutines.CancellationException
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

class NearbyPlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val places = PlacesRepository()
    private val location = LocationProvider(application)

    private val _state = MutableStateFlow<NearbyState>(NearbyState.Idle)
    val state: StateFlow<NearbyState> = _state.asStateFlow()

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
                NearbyState.Error("Couldn't load nearby places. Check your connection.")
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
    }
}
