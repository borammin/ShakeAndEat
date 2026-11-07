package com.shakeandeat.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shakeandeat.data.AuthRepository
import com.shakeandeat.data.AuthValidation
import com.shakeandeat.data.toAuthMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
)

/**
 * Holds the request state, so a rotation mid-request neither loses the spinner nor repeats the call.
 * The typed text lives in the screen with rememberSaveable. On success nothing is emitted here:
 * the auth listener in SessionViewModel moves the app to the Groups screen.
 */
class LoginViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) {
        val problem = AuthValidation.emailError(email)
            ?: if (password.isEmpty()) "Enter your password." else null
        if (problem != null) return fail(problem)
        run(registering = false) { repository.signIn(AuthValidation.normalizeEmail(email), password) }
    }

    fun register(name: String, email: String, password: String) {
        val problem = AuthValidation.nameError(name)
            ?: AuthValidation.emailError(email)
            ?: AuthValidation.passwordError(password)
        if (problem != null) return fail(problem)
        run(registering = true) {
            repository.register(name.trim(), AuthValidation.normalizeEmail(email), password)
        }
    }

    fun resetPassword(email: String) {
        val problem = AuthValidation.emailError(email)
        if (problem != null) return fail("Enter your email above, then tap Forgot password.")
        if (_state.value.loading) return
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                repository.sendPasswordReset(AuthValidation.normalizeEmail(email))
                // Same message whether or not the account exists.
                _state.value = LoginUiState(info = "If an account exists for that email, a reset link is on its way.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = LoginUiState(error = e.toAuthMessage(registering = false))
            }
        }
    }

    /** Called when the person edits a field, so a stale error does not linger. */
    fun clearMessages() {
        val current = _state.value
        if (!current.loading && (current.error != null || current.info != null)) {
            _state.value = LoginUiState()
        }
    }

    private fun fail(message: String) {
        _state.value = LoginUiState(error = message)
    }

    private fun run(registering: Boolean, block: suspend () -> Unit) {
        if (_state.value.loading) return
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                block()
                _state.value = LoginUiState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = LoginUiState(error = e.toAuthMessage(registering))
            }
        }
    }
}
