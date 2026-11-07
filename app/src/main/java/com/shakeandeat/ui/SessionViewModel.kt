package com.shakeandeat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.shakeandeat.data.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    val user: StateFlow<FirebaseUser?> = repository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.currentUser)

    fun signOut() = repository.signOut()
}
