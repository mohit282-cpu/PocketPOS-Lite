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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> _event.emit(AuthEvent.Success)
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, businessName: String, phone: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signUp(email, password, fullName, businessName, phone)) {
                is Resource.Success -> _event.emit(AuthEvent.Success)
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                else -> Unit
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resetPassword(email)) {
                is Resource.Success -> {
                    updateState { it.copy(isLoading = false, error = "Reset link sent to your email") }
                }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
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
