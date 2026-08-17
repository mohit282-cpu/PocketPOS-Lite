package com.example.pocketpos_lite.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketpos_lite.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<SplashEvent>()
    val event = _event.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val user = authRepository.getCurrentSession()
            if (user != null) {
                _event.emit(SplashEvent.Authenticated)
            } else {
                _event.emit(SplashEvent.NotAuthenticated)
            }
        }
    }

    sealed class SplashEvent {
        object Authenticated : SplashEvent()
        object NotAuthenticated : SplashEvent()
    }
}
