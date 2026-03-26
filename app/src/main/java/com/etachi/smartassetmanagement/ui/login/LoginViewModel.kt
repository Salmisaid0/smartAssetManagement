package com.etachi.smartassetmanagement.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etachi.smartassetmanagement.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.Typography.dagger

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Events to communicate with the Activity
    private val _loginState = MutableSharedFlow<LoginState>()
    val loginState: SharedFlow<LoginState> = _loginState

    fun onLoginClick(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _loginState.emit(LoginState.Error("Please fill all fields")) }
            return
        }

        viewModelScope.launch {
            _loginState.emit(LoginState.Loading)

            val result = authRepository.login(email, password)

            if (result.isSuccess) {
                _loginState.emit(LoginState.Success)
            } else {
                _loginState.emit(LoginState.Error(result.exceptionOrNull()?.message ?: "Unknown Error"))
            }
        }
    }

    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}