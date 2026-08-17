package com.example.pocketpos_lite.feature.auth

import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.core.common.BaseViewModel
import com.example.pocketpos_lite.core.common.Resource
import com.example.pocketpos_lite.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<AuthUiState>(AuthUiState()) {

    private val _event = MutableSharedFlow<AuthEvent>()
    val event = _event.asSharedFlow()

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun formatAuthError(rawMessage: String?, defaultMsg: String): String {
        val msg = rawMessage?.lowercase() ?: return defaultMsg
        return when {
            msg.contains("over_email_send_rate_limit") || msg.contains("rate limit") ->
                "Too many registration requests. Please wait a few minutes before trying again."
            msg.contains("user_already_exists") || msg.contains("already registered") || msg.contains("already in use") ->
                "An account with this email address already exists. Try logging in."
            msg.contains("invalid_credentials") || msg.contains("invalid login") ->
                "Invalid email or password. Please double-check your details."
            msg.contains("weak_password") || msg.contains("at least 6 characters") ->
                "Password is too weak. Please use at least 6 characters."
            msg.contains("unable to resolve host") || msg.contains("failed to connect") || msg.contains("network") ->
                "Network connection issue. Please check your internet connection."
            else -> rawMessage?.substringBefore("\n")?.takeIf { it.isNotBlank() } ?: defaultMsg
        }
    }

    fun signUp(email: String, password: String, fullName: String, businessName: String, phone: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signUp(email, password, fullName, businessName, phone)) {
                is Resource.Success -> _event.emit(AuthEvent.Success)
                is Resource.Error -> {
                    val cleanError = formatAuthError(result.message, "Registration failed. Please try again.")
                    updateState { it.copy(isLoading = false, error = cleanError) }
                }
                else -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> _event.emit(AuthEvent.Success)
                is Resource.Error -> {
                    val cleanError = formatAuthError(result.message, "Login failed. Please check your credentials.")
                    updateState { it.copy(isLoading = false, error = cleanError) }
                }
                else -> Unit
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resetPassword(email)) {
                is Resource.Success -> {
                    updateState { it.copy(isLoading = false, error = "Reset link sent to your email!") }
                }
                is Resource.Error -> {
                    val cleanError = formatAuthError(result.message, "Failed to send reset link.")
                    updateState { it.copy(isLoading = false, error = cleanError) }
                }
                else -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _event.emit(AuthEvent.Logout)
        }
    }

    sealed class AuthEvent {
        object Success : AuthEvent()
        object Logout : AuthEvent()
    }
}
